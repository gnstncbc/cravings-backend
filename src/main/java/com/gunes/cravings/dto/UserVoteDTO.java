package com.gunes.cravings.dto;

import com.gunes.cravings.model.VoteType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVoteDTO {
    private Long userVoteId; // ID of the UserVote record itself
    private Long userId;
    private String userEmail;
    private String userFirstname; // Added for more context
    private Long matchId;
    private VoteType voteType;
    // We could add a timestamp here if UserVote entity had one
} 