package com.family.shizi.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public class ShiziRouteTest {
    @Test
    public void routesAreUniqueAndHomeStartsNavigation() {
        List<String> routes = Arrays.stream(ShiziRoute.values())
                .map(ShiziRoute::getRoute)
                .collect(Collectors.toList());
        Set<String> uniqueRoutes = new HashSet<>(routes);

        assertEquals(8, routes.size());
        assertEquals(8, uniqueRoutes.size());
        assertEquals("home", ShiziRoute.Companion.getStartDestination());
        assertTrue(routes.containsAll(Arrays.asList(
                "home", "learn", "practice", "result", "parent", "stage_test", "learned", "profile")));
    }
}
