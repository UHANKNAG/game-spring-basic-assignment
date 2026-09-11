package com.gamebasic.game.service;

import com.gamebasic.common.exception.GameFinishedException;
import com.gamebasic.common.exception.GameNotFoundException;
import com.gamebasic.game.dto.*;
import com.gamebasic.game.entity.Game;
import com.gamebasic.game.repository.GameRepository;
import com.gamebasic.runcard.dto.CardResponse;
import com.gamebasic.runcard.dto.RunCardRequest;
import com.gamebasic.runcard.entity.RunCard;
import com.gamebasic.runcard.repository.DeckCount;
import com.gamebasic.runcard.repository.RunCardRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final RunCardRepository runCardRepository;

    @Transactional
    public GameDetailResponse createGame(CreateRequest request) {
        Game game = gameRepository.save(new Game(request.getPlayerName()));
        saveDeck(game, request.getDeck());
        List<RunCard> cards = runCardRepository.findAllByGameOrderByIdAsc(game);
        List<CardResponse> deck = new ArrayList<>();
        for (RunCard card : cards) {
            deck.add(new CardResponse(card.getId(), card.getCardType(), card.getAcquiredFloor()));
        }
        return new GameDetailResponse(
            game.getId(),
            game.getPlayerName(),
            game.getCurrentHp(),
            game.getCurrentFloor(),
            game.getPhase(),
            game.getStatus(),
            game.getCreatedAt(),
            game.getUpdatedAt(),
            deck
        );
    }

    private void saveDeck(Game game, List<RunCardRequest> deck) {
        List<RunCard> cards = new ArrayList<>();
        for (RunCardRequest card : deck) {
            cards.add(new RunCard(game, card.getCardType(), card.getAcquiredFloor()));
        }
        runCardRepository.saveAll(cards);
    }

    private Game findGame(Long gameId) {
        return gameRepository.findById(gameId)
            .orElseThrow(() -> new GameNotFoundException(gameId));
    }

    @Transactional
    public GameDetailResponse updateProgress(Long gameId, ProgressRequest request) {
        Game game = findGame(gameId);

        if (game.isFinished()) {
            throw new GameFinishedException(gameId);
        }

        game.updateProgress(
            request.getCurrentHp(),
            request.getCurrentFloor(),
            request.getPhase(),
            request.getStatus()
        );
        // 요청의 deck은 저장할 덱 전체이므로 기존 카드를 모두 지우고 요청 순서대로 다시 저장합니다.
        runCardRepository.deleteAllByGame(game);
        saveDeck(game, request.getDeck());
        List<RunCard> cards = runCardRepository.findAllByGameOrderByIdAsc(game);
        List<CardResponse> deck = new ArrayList<>();
        for (RunCard card : cards) {
            deck.add(new CardResponse(card.getId(), card.getCardType(), card.getAcquiredFloor()));
        }
        return new GameDetailResponse(
            game.getId(),
            game.getPlayerName(),
            game.getCurrentHp(),
            game.getCurrentFloor(),
            game.getPhase(),
            game.getStatus(),
            game.getCreatedAt(),
            game.getUpdatedAt(),
            deck
        );
    }

    // TODO (Lv 7): 게임 목록 조회. 주석을 풀고 구현하세요.
     @Transactional(readOnly = true)
     public List<GameSummaryResponse> getGames() {
         List<Game> games = gameRepository.findAllByOrderByIdDesc();

         // DB 조회 한 번에 game ID 별 card Count 객체를 List로 담아옴
         List<DeckCount> counts = runCardRepository.countByGames(games);

         // 키, 값 형태의 Map으로 관리하여 game Id만 입력하면 card Count가 바로 나오도록
         Map<Long, Long> deckCountOfGames = counts.stream()
                 .collect(Collectors.toMap(DeckCount::getGameId, DeckCount::getCardCount));

         return games.stream()
                .map(game -> new GameSummaryResponse(
                        game.getId(),
                        game.getPlayerName(),
                        game.getCurrentHp(),
                        game.getCurrentFloor(),
                        game.getPhase(),
                        game.getStatus(),
                        // 카드가 0장인 게임은 GROUP BY 결과에 없어서 Map 에 key 가 없다 -> getOrDefault 로 0L 대체
                        // Math.toIntExact 로 Long -> int (Integer 오토박싱은 Java 가 알아서)
                        Math.toIntExact(deckCountOfGames.getOrDefault(game.getId(), 0L)),
                        game.getCreatedAt(),
                        game.getUpdatedAt()

                )).toList();
     }

    // TODO (Lv 7): 게임 상세 조회. 주석을 풀고 구현하세요.
     @Transactional(readOnly = true)
     public GameDetailResponse getGame(Long gameId) {
        Game game = findGame(gameId);

        List<RunCard> cards = runCardRepository.findAllByGameOrderByIdAsc(game);
        List<CardResponse> deck = new ArrayList<>();
        for (RunCard card : cards) {
            deck.add(new CardResponse(card.getId(), card.getCardType(), card.getAcquiredFloor()));
        }

        return new GameDetailResponse(
                game.getId(),
                game.getPlayerName(),
                game.getCurrentHp(),
                game.getCurrentFloor(),
                game.getPhase(),
                game.getStatus(),
                game.getCreatedAt(),
                game.getUpdatedAt(),
                deck
            );
     }

    // TODO (Lv 8): 플레이어 이름 변경 — 변경 감지로 수정
    @Transactional
    public void renameGame(Long gameId, @Valid RenameRequest request) {
        Game game = findGame(gameId);

        game.rename(request.getPlayerName());
    }

    // TODO (Lv 8): 게임 삭제
    @Transactional
    public void deleteGame(Long gameId) {
        Game game = findGame(gameId);

        runCardRepository.deleteAllByGame(game);
        gameRepository.delete(game);
    }


}
