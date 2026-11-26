
package com.garmentqc.system.security;

import javax.sql.DataSource;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.garmentqc.system.repository.UserRepository;
import com.zaxxer.hikari.HikariDataSource;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

        private final UserRepository userRepository;
        private final DataSource dataSource;
        private final HikariDataSource hikariDataSource;

        @Override
        public UserDetails loadUserByUsername(String username)
                        throws UsernameNotFoundException {

                return userRepository.findByUsername(username)
                                .orElseThrow(() -> new UsernameNotFoundException("Username tidak ditemukan"));
        }

}
