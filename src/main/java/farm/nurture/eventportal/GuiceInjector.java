/*
 *  Copyright 2023 NURTURE AGTECH PVT LTD
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package farm.nurture.eventportal;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class GuiceInjector {

    private static GuiceInjector instance;

    private Injector injector;

    public static GuiceInjector getInstance() {
        if (null != instance)
            return instance;
        synchronized (GuiceInjector.class.getName()) {
            if (null != instance)
                return instance;
            instance = new GuiceInjector();
        }
        return instance;
    }

    private GuiceInjector() {
        injector = Guice.createInjector(new DIModule());
    }

    public Injector getInjector() {
        return injector;
    }

}
