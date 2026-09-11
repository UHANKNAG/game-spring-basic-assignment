package com.gamebasic.runcard.repository;

import com.gamebasic.game.entity.Game;
import com.gamebasic.runcard.entity.RunCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RunCardRepository extends JpaRepository<RunCard, Long> {
    List<RunCard> findAllByGameOrderByIdAsc(Game game);

    void deleteAllByGame(Game game);

    // TODO (Lv 11): @Query 작성
    @Query("""
        SELECT new com.gamebasic.runcard.repository.DeckCount(rc.game.id, COUNT(rc.id))
        FROM RunCard rc
        WHERE  rc.game in :games
        GROUP BY rc.game.id
    """)
    List<DeckCount> countByGames(List<Game> games);
}
