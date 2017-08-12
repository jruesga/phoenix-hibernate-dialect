/*
 * Copyright (C) 2017 Jorge Ruesga
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ruesga.phoenix.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class JpaEntityManager {

    private static JpaEntityManager instance;
    private final EntityManagerFactory factory;

    private JpaEntityManager() {
        factory = Persistence.createEntityManagerFactory("jpa");
    }

    public static final synchronized JpaEntityManager getInstance() {
        if (instance == null) {
            instance = new JpaEntityManager();
        }
        return instance;
    }

    public EntityManager createEntityManager() {
        return factory.createEntityManager();
    }

    public void close() {
        factory.close();
    }
}
