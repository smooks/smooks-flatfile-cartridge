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

import org.smooks.cartridges.flatfile.variablefield.VariableFieldRecordParser;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex record parser.
 * <p/>
 * If there are no groups defined in the regexPattern this parser will use the
 * pattern to split the record into fields. If groups are defined, it will
 * extract the record field data from the groups defined in the pattern.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class RegexParser<T extends RegexParserFactory> extends VariableFieldRecordParser<T> {

    private BufferedReader reader;
    private StringBuilder readerBuffer;
    private int groupCount;

    public void setDataSource(InputSource source) {
        Reader reader = source.getCharacterStream();

        if (reader == null) {
            throw new IllegalStateException(
                    "Invalid InputSource type supplied to RegexParser.  Must contain a Reader instance.");
        }

        this.reader = new BufferedReader(reader);
        this.readerBuffer = new StringBuilder();
        this.groupCount = getFactory().getRegexPattern().matcher("").groupCount();
    }

    @Override
    public List<String> nextRecordFieldValues() throws IOException {
        T factory = getFactory();
        Pattern pattern = factory.getRegexPattern();

        readerBuffer.setLength(0);
        factory.readRecord(reader, readerBuffer, (getRecordCount() + 1));

        if (readerBuffer.length() == 0) {
            return null;
        }

        if (groupCount > 0) {
            String recordString = readerBuffer.toString();
            List<String> fields = new ArrayList<String>();
            Matcher matcher = pattern.matcher(recordString);

            if (matcher.matches()) {
                for (int i = 0; i < matcher.groupCount(); i++) {
                    String fieldValue = matcher.group(i + 1);
                    if (fieldValue != null) {
                        fields.add(fieldValue);
                    }
                }
            } else {
                // Add the full record text as the only field value
                fields.add(recordString);
            }

            return fields;
        } else {
            return Arrays.asList(pattern.split(readerBuffer.toString()));
        }
    }
}
