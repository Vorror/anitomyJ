/*
 * Copyright (c) 2014-2016, Eren Okka
 * Copyright (c) 2016, Paul Miller
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dgtlrepublic.model.utility;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dgtlrepublic.anitomyj.AnitomyJ;
import com.dgtlrepublic.anitomyj.Element;
import com.dgtlrepublic.model.test.DataTest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * A simple utility class that can convert Okka's <a href="https://github.com/erengy/anitomy/blob/master/test/data.json">data.json</a>
 * into something that's more easily testable. We don't use data.json as our validator/test-cases.json because not even
 * the original Anitomy parses/extracts every field inside data.json.
 * <p>
 * Usage: Simply run the main class to create the test-cases.json. Then you must hand verify that each entry actually
 * produces the correct result(Using the original Anitomy library).
 * <p>
 * This class was only really used to give me a starting point, and I thought it was useful to commit. We won't actually
 * rebuild the test-cases.json every time( Otherwise we would be testing nothing ;D ). New entries will just be ran through
 * this class, hand verified/tuned and then appended to the official test-cases.json to make valid test cases.
 *
 * @author Paul Miller
 */
public class DataJsonConverter {
    /**
     * Parses an anime file name and returns a map that can be used for test cases.
     *
     * @param fileName the anime file name
     * @return the parsed map
     *
     * @see #toTestCaseMap(Map)
     */
    public static HashMap<String, Object> toTestCaseMap(String fileName) {
        return toTestCaseMap(new HashMap<String, String>(1) {{
            put("file_name", fileName);
        }});
    }

    /**
     * Parses a map and returns another map that can be used for test cases.
     *
     * @param entry the entry map
     * @return the parsed map
     *
     * @see #toTestCaseMap(String)
     */
    @SuppressWarnings("unchecked")
    private static HashMap<String, Object> toTestCaseMap(Map entry) {
        List<Element> results = AnitomyJ.parse((String) entry.get("file_name"));
        HashMap<String, Object> result = new HashMap<>();
        HashMap<String, Object> element = new HashMap<>();

        /** add parsed results to our result test case */
        results.forEach(e -> {
            Object o = element.get(e.getCategory().name());
            if (o != null && o instanceof List) {
                ((ArrayList<Object>) o).add(e.getValue());
            } else if (o != null) {
                element.put(e.getCategory().name(), Arrays.asList(o, e.getValue()));
            } else {
                element.put(e.getCategory().name(), e.getValue());
            }
        });

        result.put("id", entry.getOrDefault("id", -1)); /** Unused */
        result.put("file_name", entry.getOrDefault("file_name", "")); /** File name */
        result.put("ignore", entry.getOrDefault("ignore", false)); /** Ignore test case */
        result.put("results", element); /** Parsing result */
        return result;
    }

    /**
     * Returns a parsed map for a json file.
     *
     * @param file the json file
     * @return the map
     */
    private static List<Map> getDataJsonMap(File file) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, new TypeReference<List<Map>>() { });
    }

    @SuppressWarnings({"unchecked", "MalformedFormatString"})
    public static void main(String args[]) throws Exception {
        List<Map> entries = getDataJsonMap(new File(DataTest.class.getResource("/data.json").getPath()));

        List<HashMap<String, Object>> testData = entries.stream()
                .map(DataJsonConverter::toTestCaseMap)
                .collect(Collectors.toList());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        File location = new File(System.getProperty("user.dir"), "test-cases-sample.json");
        Files.write(location.toPath(),
                    String.format(
                            "// This line was purposely added to break json parsers, and force users to this message ;)" +
                                    "%n// This file must be hand verified for correctness(generally only new entries), if this file is simply renamed to result.json then nothing is tested." +
                                    "%n// Correctness means verifying the result against the actual C++ library(not data.json!).%n%s",
                            objectMapper.writeValueAsString(testData))
                            .getBytes(),
                    StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        System.out.println(String.format("Created %s test cases inside: %s", testData.size(), location));
    }
}
