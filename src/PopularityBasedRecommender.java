import java.util.*;

import static java.util.stream.Collectors.*;

/** Popularity‑based recommender implementation. */
class PopularityBasedRecommender<T extends Item> extends RecommenderSystem<T> {
    private static final int POPULARITY_THRESHOLD = 100;
    public PopularityBasedRecommender(Map<Integer, User> users,
                                      Map<Integer, T> items,
                                      List<Rating<T>> ratings) {
        super(users, items, ratings);
    }

    @Override
    public List<T> recommendTop10(int userId) {
        Set<Integer> userRatedItems = ratingsByUser.getOrDefault(userId, Collections.emptyList())
                .stream()
                .map(Rating::getItemId)
                .collect(toSet());

        return items.values().stream()
                .filter(item -> getItemRatingsCount(item.getId()) >= POPULARITY_THRESHOLD)
                .filter(item -> !userRatedItems.contains(item.getId()))
                .sorted(Comparator.comparingDouble((T item) -> getItemAverageRating(item.getId())).reversed()
                        .thenComparing(Comparator.comparingInt((T item) -> getItemRatingsCount(item.getId())).reversed())
                        .thenComparing(Item::getName))
                .limit(NUM_OF_RECOMMENDATIONS)
                .collect(toList());
    }

    public double getItemAverageRating(int itemId) {
        List<Rating<T>> itemRatings = ratingsByItem.get(itemId);
        if (itemRatings == null || itemRatings.isEmpty()) return 0.0;
        return itemRatings.stream()
                .mapToDouble(Rating::getRating)
                .average()
                .orElse(0.0);
    }
    
    public int getItemRatingsCount(int itemId) {
        List<Rating<T>> itemRatings = ratingsByItem.get(itemId);
        return itemRatings == null ? 0 : itemRatings.size();
    }

}
