package io.github.liamtkehoe.nhl_pickem.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminAccess {
    private final Set<String> admins;

    public AdminAccess(@Value("${pickem.admins:}") List<String> admins) {
        this.admins = admins.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    public boolean isAdmin(String username) {
        return username != null && admins.contains(username.trim().toLowerCase(Locale.ROOT));
    }
}
