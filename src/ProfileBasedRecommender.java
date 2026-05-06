import java.util.*;

import static java.util.stream.Collectors.*;

/** Profile‑based recommender implementation. */
class ProfileBasedRecommender<T extends Item> extends RecommenderSystem<T> {
    public ProfileBasedRecommender(Map<Integer, User> users,
                                   Map<Integer, T> items,
                                   List<Rating<T>> ratings) {
        super(users, items, ratings);
    }

    private static class ItemStats {
        final int itemId;
        final double avgRating;
        final int count;
        ItemStats(int itemId, double avgRating, int count) {
            this.itemId = itemId; this.avgRating = avgRating; this.count = count;
        }
    }

    @Override
    public List<T> recommendTop10(int userId) {
        Set<Integer> matchingUserIds = getMatchingProfileUsers(userId).stream()
                .map(User::getId)
                .collect(toSet());

        Map<Integer, List<Rating<T>>> matchingRatingsByItem = ratings.stream()
                .filter(r -> matchingUserIds.contains(r.getUserId()))
                .collect(groupingBy(Rating::getItemId));

        Set<Integer> userRatedItems = ratingsByUser.getOrDefault(userId, Collections.emptyList())
                .stream()
                .map(Rating::getItemId)
                .collect(toSet());

        return matchingRatingsByItem.entrySet().stream()
                .filter(e -> e.getValue().size() >= 5)
                .filter(e -> !userRatedItems.contains(e.getKey()))
                .map(e -> new ItemStats(
                        e.getKey(),
                        e.getValue().stream().mapToDouble(Rating::getRating).average().orElse(0.0),
                        e.getValue().size()
                ))
                .sorted(Comparator.comparingDouble((ItemStats s) -> s.avgRating).reversed()
                        .thenComparing(Comparator.comparingInt((ItemStats s) -> s.count).reversed())
                        .thenComparing(s -> items.get(s.itemId).getName()))
                .limit(NUM_OF_RECOMMENDATIONS)
                .map(s -> items.get(s.itemId))
                .collect(toList());
    }

    public List<User> getMatchingProfileUsers(int userId) {
        User targetUser = users.get(userId);
        if (targetUser == null) return Collections.emptyList();
        return users.values().stream()
                .filter(u -> u.getId() != userId)  // ← ADD THIS LINE
                .filter(u -> u.getGender().equals(targetUser.getGender()))
                .filter(u -> Math.abs(u.getAge() - targetUser.getAge()) <= 5)
                .collect(toList());
    }

}
