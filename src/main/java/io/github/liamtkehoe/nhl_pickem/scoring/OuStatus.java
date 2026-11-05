package io.github.liamtkehoe.nhl_pickem.scoring;

public enum OuStatus {
    PENDING,        // no games played or no line yet
    LEADING,        // on pace for the picked side
    TRAILING,       // on pace for the other side
    CLINCHED_WIN,   // can no longer lose
    CLINCHED_LOSS,  // can no longer win
    PUSH            // exactly on the line
}
