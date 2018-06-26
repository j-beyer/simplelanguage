package com.oracle.truffle.sl.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import com.oracle.truffle.api.object.*;

public class SLShapeWrapper {

    public static SLShapeWrapper getWrapperForShape(Shape shape) {
        if (!wrapperMap.containsKey(shape)) {
            wrapperMap.put(shape, new SLShapeWrapper(shape));
        }

        return wrapperMap.get(shape);
    }

    public static Shape inlineSubshape(Shape shape, Shape subshape, Integer position, String propertyName) {
        for (Property property : subshape.getProperties()) {
            if (!(property.getKey() instanceof String)) {
                throw new AssertionError("Shape key is not a string.");
            }
            String key = propertyName + "_" + (String)property.getKey();
            Shape.Allocator allocator = shape.allocator();
            shape = shape.addProperty(Property.create(key, allocator.locationForType(Object.class), 0));
        }

        System.out.println("Inlining result:\n" + shape.toString());
        return shape;
    }

    private static Map<Shape, SLShapeWrapper> wrapperMap = new HashMap<>();

    private Shape shape;
    private Map<Integer, Map<Shape, Integer>> observations;
    private Map<Integer, Set<Shape>> transformations;

    private SLShapeWrapper(Shape _shape) {
        shape = _shape;
        observations = new HashMap<>();
        transformations = new HashMap<>();
    }

    public boolean isTransformation(Integer position, Shape subshape) {
        return transformations.containsKey(position) && transformations.get(position).contains(subshape);
    }

    public void observeSubshape(Integer position, Shape subshape) {
        if (!observations.containsKey(position)) {
            observations.put(position, new HashMap<>());
        }

        Integer numberObserved = observations.get(position).getOrDefault(subshape, 0);
        numberObserved += 1;
        observations.get(position).put(subshape, numberObserved);

        if (numberObserved == 7) {
            if (!transformations.containsKey(position)) {
                transformations.put(position, new HashSet<>());
            }
            transformations.get(position).add(subshape);
            System.out.println("Transformation recorded:\n" + 
                shape.toString() + "\n" +
                "at position " + position.toString() + "\n" +
                subshape.toString());
        }
    }
}