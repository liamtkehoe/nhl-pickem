package io.github.liamtkehoe.nhl_pickem.service;

import java.util.Map;

public record OULinesImport(int season, String source, String asOf, Map<String, Double> lines) {
}
