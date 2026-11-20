package io.github.liamtkehoe.nhl_pickem.nhl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StatsPage<T>(List<T> data, int total) {
}
