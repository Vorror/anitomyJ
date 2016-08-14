/*
 * Copyright (c) 2014-2016, Eren Okka
 * Copyright (c) 2016, Paul Miller
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dgtlrepublic.model.test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.dgtlrepublic.anitomyj.AnitomyJ;
import com.dgtlrepublic.anitomyj.Element;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

// TODO: 8/14/2016 fix out test situation *shrugs* 
public class DataTest {

    @Test
    public void testData() {
        List<Map> entries = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            File src = new File(DataTest.class.getResource("/data.json").getPath());
            entries = mapper.readValue(src, new TypeReference<List<Map>>() { });
            System.out.println(entries);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Map entry : entries) {
            String  file_name1 = (String)entry.get("file_name");
            List<Element> file_name = AnitomyJ.parse(file_name1);
            System.out.println(file_name1);
            file_name.forEach(element -> System.out.println("\t"+element));
        }
    }
}
