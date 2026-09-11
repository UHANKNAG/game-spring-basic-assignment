package com.gamebasic.runcard.repository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DeckCount {
    private final Long gameId;
    private final Long cardCount;
}
