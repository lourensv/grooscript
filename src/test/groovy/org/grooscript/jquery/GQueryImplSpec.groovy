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
package org.grooscript.jquery

import org.grooscript.rx.Observable
import spock.lang.Specification

class GQueryImplSpec extends Specification {

    def 'bind all properties'() {
        given:
        GroovySpy(GQueryList, global: true)
        def item = new Expando(namep: 'nameValue', idp: 'idValue', groupp: 'groupValue')
        def binded = 0
        hasResults.bind(item, 'namep') >> { binded++; hasResults }
        hasResults.bind(item, 'idp') >> { binded++; hasResults }
        hasResults.bind(item, 'groupp') >> { binded++; hasResults }

        when:
        gQueryImpl.bindAllProperties(item)

        then:
        1 * GQueryList.of('#namep') >> hasNotResults
        2 * GQueryList.of('#idp') >> hasResults
        1 * GQueryList.of('#groupp') >> hasNotResults
        2 * GQueryList.of("[name='namep']") >> hasResults
        1 * GQueryList.of("[name='idp']") >> hasNotResults
        1 * GQueryList.of("[name='groupp']") >> hasNotResults
        1 * GQueryList.of("input:radio[name='namep']") >> hasNotResults
        1 * GQueryList.of("input:radio[name='idp']") >> hasNotResults
        2 * GQueryList.of("input:radio[name='groupp']") >> hasResults
        binded == 3
        0 * _
    }

    def 'bind all properties with parent'() {
        given:
        GroovySpy(GQueryList, global: true)
        def item = new Expando(namep: 'nameValue', idp: 'idValue', groupp: 'groupValue')
        def binded = 0
        def parent = Mock(GQueryList)
        hasResults.bind(item, 'namep') >> { binded++; hasResults }
        hasResults.bind(item, 'idp') >> { binded++; hasResults }
        hasResults.bind(item, 'groupp') >> { binded++; hasResults }

        when:
        gQueryImpl.bindAllProperties(item, parent)

        then:
        6 * GQueryList.of(hasNotResults) >> hasNotResults
        6 * GQueryList.of(hasResults) >> hasResults
        1 * parent.find('#namep') >> hasNotResults
        2 * parent.find('#idp') >> hasResults
        1 * parent.find('#groupp') >> hasNotResults
        2 * parent.find("[name='namep']") >> hasResults
        1 * parent.find("[name='idp']") >> hasNotResults
        1 * parent.find("[name='groupp']") >> hasNotResults
        1 * parent.find("input:radio[name='namep']") >> hasNotResults
        1 * parent.find("input:radio[name='idp']") >> hasNotResults
        2 * parent.find("input:radio[name='groupp']") >> hasResults
        binded == 3
        0 * _
    }

    def 'bind methods'() {
        given:
        GroovySpy(GQueryList, global: true)
        def instance = new WithEvens()
        int click = 0, submit = 0, change = 0
        hasResults.onEvent('click', _) >> { click++; hasResults }
        hasResults.onEvent('submit', _) >> { submit++; hasResults }
        hasResults.onChange(_) >> { change++; hasResults }

        when:
        gQueryImpl.attachMethodsToDomEvents(instance)

        then:
        2 * GQueryList.of('#id1') >> hasResults
        2 * GQueryList.of('#id2') >> hasResults
        2 * GQueryList.of('#id3') >> hasResults
        click == 1 && submit == 1 && change == 1
    }

    def 'exists selector'() {
        given:
        GroovySpy(GQueryList, global: true)
        def selector = 'selector'

        when:
        gQueryImpl.existsSelector(selector) == true

        then:
        1 * GQueryList.of(selector) >> hasResults
    }

    def 'exists selector with parent'() {
        given:
        GroovySpy(GQueryList, global: true)
        def parent = Mock(GQueryList)
        def selector = 'selector'

        when:
        gQueryImpl.existsSelector(selector, parent) == true

        then:
        1 * parent.find(selector) >> hasResults
    }

    def 'chain methods'() {
        given:
        GroovySpy(GQueryList, global: true)
        def queryList = Mock(GQueryList)
        def cl = { -> println 'Hello!' }

        when:
        def result = gQueryImpl.focusEnd(selector).withResultList(cl).hasResults()

        then:
        1 * GQueryList.of(selector) >> queryList
        1 * queryList.focusEnd() >> queryList
        1 * queryList.withResultList(cl) >> queryList
        1 * queryList.hasResults() >> true
        result == true
    }

    def 'observe event'() {
        given:
        GroovySpy(Observable, global: true)
        GroovySpy(GQueryList, global: true)
        def observable = Stub(Observable)
        def queryList = Mock(GQueryList)
        def nameEvent = 'even'
        def data = [a: 1]

        when:
        def result = gQueryImpl.observeEvent(selector, nameEvent, data)

        then:
        1 * Observable.listen() >> observable
        1 * GQueryList.of(selector) >> queryList
        1 * queryList.on(nameEvent, data, _)
        result == observable
    }

    def 'check gQueryList methods'() {
        given:
        def closure = { -> true}
        def gq = new GQueryList(selector)

        expect:
        gq.list == null
        gq.focusEnd() == null
        gq.hasResults() == false
        gq.onChange(closure) == null
        gq.onEvent('name', closure) == null
        gq.bind(this, 'name', closure) == null
        gq.withResultList(closure) == null
    }

    class WithEvens {
        def id1Click() {}
        def id2Submit() {}
        def id3Change() {}
    }

    private selector = 'select'
    private GQueryImpl gQueryImpl = new GQueryImpl()
    private hasResults = Stub(GQueryList) { it.hasResults() >> true }
    private hasNotResults = Stub(GQueryList) { it.hasResults() >> false }
}
