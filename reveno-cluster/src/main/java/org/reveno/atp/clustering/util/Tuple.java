/**
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package org.reveno.atp.clustering.util;

public class Tuple<V1,V2> {
    private V1 val1;
    private V2 val2;

    public Tuple(V1 val1, V2 val2) {
        this.val1=val1;
        this.val2=val2;
    }

    public V1 getVal1() {
        return val1;
    }

    public void setVal1(V1 val1) {
        this.val1=val1;
    }

    public V2 getVal2() {
        return val2;
    }

    public void setVal2(V2 val2) {
        this.val2=val2;
    }

    public String toString() {
        return val1 + " : " + val2;
    }
}
