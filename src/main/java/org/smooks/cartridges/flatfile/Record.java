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
package org.smooks.cartridges.flatfile;

import org.smooks.assertion.AssertArgument;

import java.util.List;

/**
 * Flat file record.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class Record {

    private String name;
    private List<Field> fields;
    private RecordMetaData recordMetaData;

    /**
     * Public constructor.
     * @param name The record name.  This will be used to create the element that will
     * enclose the record field elements.
     * @param fields The record fields.
     * @param recordMetaData Record metadata.
     */
    public Record(String name, List<Field> fields, RecordMetaData recordMetaData) {
        AssertArgument.isNotNullAndNotEmpty(name, "name");
        AssertArgument.isNotNullAndNotEmpty(fields, "fields");
        this.name = name;
        this.fields = fields;
        this.recordMetaData = recordMetaData;
    }

    /**
     * Get the name of the record.
     * @return The record name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the record fields.
     * @return The record fields.
     */
    public List<Field> getFields() {
        return fields;
    }

    /**
     * Get the record metadata.
     * @return The record metadata.
     */
    public RecordMetaData getRecordMetaData() {
        return recordMetaData;
    }
}
