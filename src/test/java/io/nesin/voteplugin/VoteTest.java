package io.nesin.voteplugin;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VoteTest {

    @Test
    void testVoteInitiatorAutomaticallyVotesYes() {
        UUID initiator = UUID.randomUUID();
        Vote vote = new Vote(null, initiator, VoteTarget.DAY);

        assertEquals(1, vote.getYesCount());
        assertEquals(0, vote.getNoCount());
        assertTrue(vote.hasVoted(initiator));
    }

    @Test
    void testDuplicateVoteRejected() {
        UUID initiator = UUID.randomUUID();
        Vote vote = new Vote(null, initiator, VoteTarget.DAY);

        // Initiator tries to vote again
        assertFalse(vote.addVote(initiator, false));
        assertEquals(1, vote.getYesCount());
        assertEquals(0, vote.getNoCount());

        // Another player votes
        UUID player2 = UUID.randomUUID();
        assertTrue(vote.addVote(player2, false));
        assertFalse(vote.addVote(player2, true));
        assertEquals(1, vote.getYesCount());
        assertEquals(1, vote.getNoCount());
    }

    @Test
    void testMultipleVotesCounting() {
        UUID initiator = UUID.randomUUID();
        Vote vote = new Vote(null, initiator, VoteTarget.CLEAR);

        vote.addVote(UUID.randomUUID(), true);
        vote.addVote(UUID.randomUUID(), true);
        vote.addVote(UUID.randomUUID(), false);

        assertEquals(3, vote.getYesCount());
        assertEquals(1, vote.getNoCount());
    }
}
