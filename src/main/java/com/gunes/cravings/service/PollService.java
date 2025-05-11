package com.gunes.cravings.service;

import com.gunes.cravings.dto.MatchDetailDTO;
import com.gunes.cravings.dto.VoteRequestDTO;
import com.gunes.cravings.exception.AlreadyVotedException;
import com.gunes.cravings.exception.ResourceNotFoundException;
import com.gunes.cravings.model.Match;
import com.gunes.cravings.model.User;
import com.gunes.cravings.model.UserVote;
import com.gunes.cravings.repository.MatchRepository;
import com.gunes.cravings.repository.UserRepository; // Assuming you might need this for the current user
import com.gunes.cravings.repository.UserVoteRepository;
import com.gunes.cravings.dto.UserVoteDTO; // Added import
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PollService {

    private final MatchRepository matchRepository;
    private final UserVoteRepository userVoteRepository;
    private final UserRepository userRepository;
    private final MatchService matchService; // To use convertToMatchDetailDTO

    @Transactional
    public MatchDetailDTO submitVote(Long matchId, VoteRequestDTO voteRequest) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUserEmail));

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        // Check if the user has already voted for this match
        userVoteRepository.findByUserAndMatch(currentUser, match).ifPresent(existingVote -> {
            throw new AlreadyVotedException("User has already voted for this match.");
        });

        // Create and save the new vote
        UserVote newUserVote = UserVote.builder()
                .user(currentUser)
                .match(match)
                .voteType(voteRequest.getVoteType())
                .build();
        userVoteRepository.save(newUserVote);

        // Update aggregate vote counts on the Match entity
        switch (voteRequest.getVoteType()) {
            case TEAM_A_WINS:
                match.setTeamAVotes(match.getTeamAVotes() + 1);
                break;
            case TEAM_B_WINS:
                match.setTeamBVotes(match.getTeamBVotes() + 1);
                break;
            case DRAW:
                match.setDrawVotes(match.getDrawVotes() + 1);
                break;
            default:
                throw new IllegalArgumentException("Invalid vote type: " + voteRequest.getVoteType());
        }
        Match updatedMatch = matchRepository.save(match);

        // Return updated match details
        return matchService.convertToMatchDetailDTO(updatedMatch); // Delegate to MatchService for DTO conversion
    }

    @Transactional(readOnly = true) // Good practice for read operations
    public MatchDetailDTO getMatchResults(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));
        // The MatchDetailDTO already contains the vote counts (teamAVotes, teamBVotes, drawVotes)
        // as populated by matchService.convertToMatchDetailDTO
        return matchService.convertToMatchDetailDTO(match);
    }   

    @Transactional(readOnly = true) // Good practice for read operations
    public MatchDetailDTO getMatchVotes(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));
        return matchService.convertToMatchDetailDTO(match);
    }

    @Transactional(readOnly = true)
    public List<UserVoteDTO> getVotesForMatchByUsers(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));
        
        List<UserVote> userVotes = userVoteRepository.findAllByMatch(match);
        
        return userVotes.stream()
                .map(this::convertToUserVoteDTO)
                .collect(Collectors.toList());
    }

    private UserVoteDTO convertToUserVoteDTO(UserVote userVote) {
        User user = userVote.getUser(); // Assuming UserVote has a getUser() method that is not lazy loaded without a session
        Match match = userVote.getMatch(); // Same assumption for match
        return new UserVoteDTO(
            userVote.getId(),
            user.getId().longValue(),
            user.getEmail(),
            user.getFirstname(),
            match.getId(),
            userVote.getVoteType()
        );
    }
} 