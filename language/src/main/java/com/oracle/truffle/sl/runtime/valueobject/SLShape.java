package com.oracle.truffle.sl.runtime.valueobject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static com.oracle.truffle.api.CompilerDirectives.*;
import static java.util.stream.Collectors.toList;

public class SLShape {

    private final List<SLShape> subShapes;
    private final List<Integer> shapeIndices;
    private int depth;

    public static SLShape of(List<SLShape> subShapes) {
        return new SLShape(subShapes);
    }

    static SLShape directAccessOf(int numElements) {
        List<SLShape> nullElements = IntStream.range(0, numElements).mapToObj(i -> (SLShape) null).collect(toList());
        return new SLShape(nullElements);
    }

    private SLShape(List<SLShape> subShapes) {
        this.subShapes = subShapes;
        this.shapeIndices = new ArrayList<>();
        this.depth = 0;
        calculateIndices();
    }

    private void calculateIndices() {
        this.shapeIndices.clear();

        int curIndex = 0;
        for (SLShape shape : subShapes) {
            this.shapeIndices.add(curIndex);
            if (shape == null) {
                curIndex += 1;
            } else {
                curIndex += shape.getNumPrimitives();
                this.depth = Math.max(this.depth, shape.getDepth() + 1);
            }
        }
        // add number of total primitves at end
        this.shapeIndices.add(curIndex);
    }

    private int getNumPrimitives() {
        return shapeIndices.get(shapeIndices.size() - 1);
    }

    Range getSubshapeRange(int index) {
        return new Range(shapeIndices.get(index), shapeIndices.get(index + 1) - 1);
    }

    int getNumFields() {
        return subShapes.size();
    }

    private int getNumFieldsUnfoldingSubshapes() {
        return subShapes.stream().map(shape -> shape == null ? 1 : shape.getNumFieldsUnfoldingSubshapes())
                .mapToInt(Integer::intValue).sum();
    }

    @TruffleBoundary
    void inlineShape(int inlinedFieldIndex, SLShape newSubShape) {
        int beginIndex = 0;
        int i = 0;
        while (beginIndex < getNumFieldsUnfoldingSubshapes() && i < getNumFields()) {

            int endIndex = beginIndex;
            if (subShapes.get(i) != null) {
                SLShape existingSubShape = subShapes.get(beginIndex);
                endIndex += existingSubShape.getNumFieldsUnfoldingSubshapes();

                if (inlinedFieldIndex >= beginIndex && inlinedFieldIndex <= endIndex) {
                    existingSubShape.inlineShape(inlinedFieldIndex - beginIndex, newSubShape);
                }
            } else {
                if (inlinedFieldIndex == beginIndex) {
                    subShapes.set(i, newSubShape);
                    break;
                }
            }
            beginIndex = endIndex + 1;
            i++;
        }

        calculateIndices();
    }

    public boolean isSubshapeAt(int index) {
        return subShapes.get(index) != null;
    }

    public SLShape getSubshape(int index) {
        if (subShapes.get(index) != null) {
            return subShapes.get(index);
        }
        throw new RuntimeException("No subshape found at index " + index);
    }

    public int getObjectStorageStart(int index) {
        return shapeIndices.get(index);
    }

    public int getDepth() {
        return depth;
    }

    /**
     * Range with inclusive begin and end
     */
    class Range {
        private final int begin;
        private final int end;

        Range(int begin, int end) {
            this.begin = begin;
            this.end = end;
        }

        int getBegin() {
            return begin;
        }

        int getEnd() {
            return end;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SLShape shape = (SLShape) o;
        return Objects.equals(subShapes, shape.subShapes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subShapes);
    }
}
