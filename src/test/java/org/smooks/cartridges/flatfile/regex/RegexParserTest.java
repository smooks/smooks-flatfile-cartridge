/*-
 * ========================LICENSE_START=================================
 * smooks-flatfile-cartridge
 * %%
 * Copyright (C) 2020 Smooks
 * %%
 * Licensed under the terms of the Apache License Version 2.0, or
 * the GNU Lesser General Public License version 3.0 or later.
 *
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-3.0-or-later
 *
 * ======================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ======================================================================
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * =========================LICENSE_END==================================
 */
package org.smooks.cartridges.flatfile.regex;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.junit.Test;
import org.smooks.Smooks;
import org.smooks.io.payload.JavaResult;
import org.smooks.io.payload.StringResult;
import org.smooks.io.payload.StringSource;
import org.smooks.support.StreamUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@SuppressWarnings("unchecked")
public class RegexParserTest {

    @Test
    public void test_01() throws IOException, SAXException {
        testHelper("01", "a|b|c\n\rd|e|f");
    }

    @Test
    public void test_02() throws IOException, SAXException {
        testHelper("02", "a|b|c\n\rd|e|f");
    }

    @Test
    public void test_03() throws IOException, SAXException {
        testHelper("03", "a|b|c\nd|e|f");
    }

    @Test
    public void test_04() throws IOException, SAXException {
        testHelper("04", "a|b|c\nd|e|f");
    }

    @Test
    public void test_05() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("/smooks-config-05.xml"));
        JavaResult result = new JavaResult();

        smooks.filterSource(new StringSource("a|b|c\n\rd|e|f"), result);

        List<FSTRecord> fstRecords = (List<FSTRecord>) result.getBean("fstRecords");

        Assert.assertEquals(2, fstRecords.size());
        Assert.assertEquals("a|b|c", fstRecords.get(0).toString());
        Assert.assertEquals("d|e|f", fstRecords.get(1).toString());
    }

    @Test
    public void test_06() throws IOException, SAXException {
        // Should result in unmatched records because the regex's do
        // not match the input...
        testHelper("06", "name|Tom|Fennelly\n\r" +
                "address|Skeagh Bridge|Tinnakill");
    }

    @Test
    public void test_07() throws IOException, SAXException {
        testHelper("07", "name|Tom|Fennelly\n\r" +
                "address|Skeagh Bridge|Tinnakill");
    }

    @Test
    public void test_08() throws IOException, SAXException {
        testHelper("08", "name|Tom|Fennelly\n\r" +
                "address|Skeagh Bridge|Tinnakill");
    }

    @Test
    public void test_09() throws IOException, SAXException {
        testHelper("09", "1|Tom|Fennelly" +
                "2|Mike|Fennelly");
    }

    @Test
    public void test_10() throws IOException, SAXException {
        testHelper("10", "10/26 03:04:21.016 A1 : EVENT=Msg_Rcvd, E_ID=1, D_ID=D1, M_ID=M1, R=23525235\n" +
                "10/26 03:04:21.032 B12 : EVENT=Msg_Sent, E_ID=2, D_ID=D1, M_ID=M1, R=34523455\n" +
                "10/26 03:04:21.040 A1 : EVENT=Msg_Rcvd, E_ID=3, D_ID=D2, M_ID=M2, R=15894578\n" +
                "10/26 03:04:22.000 A1 : EVENT=Filler\n" +
                "10/26 03:04:21.076 A30 : EVENT=Msg_Rcvd, E_ID=7, D_ID=D2, M_ID=M4, R=97847854");
    }

    public void testHelper(String config, String message) throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("/smooks-config-" + config + ".xml"));
        String expected = StreamUtils.readStreamAsString(getClass().getResourceAsStream("/expected-" + config + ".xml"), "UTF-8");

        StringResult result = new StringResult();
        smooks.filterSource(new StringSource(message), result);

        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(expected, result.toString());
    }
}
