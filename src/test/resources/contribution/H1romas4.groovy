/*
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
 */
package contribution

class Test {
    def field1 = 0
    def field2 = 0
    Test() {
        // OK
        (1..10).each {
            field1 += it
        }
        println field1
        // ReferenceError: "field2" is not defined.
        for(def i = 1; i <= 10; i++) {
            field2 += i
            // OK
            // this.field2 += i
        }
        println field2
    }
}

new Test()