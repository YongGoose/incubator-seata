/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.console.security;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import javax.annotation.PostConstruct;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom user service
 *
 */
@Service
public class CustomUserDetailsServiceImpl implements UserDetailsService {

    private Console console;
    private User user;

    /**
     * Init.
     */
    @PostConstruct
    public void init() throws IOException {
        String envUsername = System.getenv("SEATA_CONSOLE_USERNAME");
        String envPassword = System.getenv("SEATA_CONSOLE_PASSWORD");

        if (envUsername != null && envPassword != null) {
            user = new User(envUsername, envPassword);
            return;
        }

        console = System.console();
        if (console == null) {
            // In an IDE, 'System.console()' returns 'null', so 'BufferedReader' is used instead.
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String username = promptInput(reader, "Username: ");
            String password = promptInput(reader, "Password: ");
            user = new User(username, password);
        } else {
            String username = getUsername();
            String password = getUserPassword();
            user = new User(username, password);
        }
    }

    private String promptInput(BufferedReader reader, String message) throws IOException {
        System.out.print(message);
        return reader.readLine();
    }

    private String getUserPassword() {
        return Arrays.toString(console.readPassword("Password: "));
    }

    private String getUsername() {
        return console.readLine("Username: ");
    }

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        if (!user.getUsername().equals(userName)) {
            throw new UsernameNotFoundException(userName);
        }
        return new CustomUserDetails(user);
    }
}
