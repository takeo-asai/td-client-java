//
// Java Client Library for Treasure Data Cloud
//
// Copyright (C) 2011 - 2012 Muga Nishizawa
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package com.treasure_data.model;

class DatabaseSpecifyRequest<T> extends AbstractRequest {
    private String databaseName;

    public DatabaseSpecifyRequest() {
    }

    public DatabaseSpecifyRequest(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaeName(String databaseName) {
        this.databaseName = databaseName;
    }

    public T withDatabaseName(String databaseName) {
        T c = (T) clone();
        c.setDatabaeName(databaseName);
        return c;
    }
}
