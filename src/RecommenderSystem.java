import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

/** Abstract generic recommender system. */
abstract class RecommenderSystem<T extends Item> {
    protected final Map<Integer, User> users;
    protected final Map<Integer, T> items;
    protected final List<Rating<T>> ratings;
    protected final Map<Integer, List<Rating<T>>> ratingsByUser;
    protected final Map<Integer, List<Rating<T>>> ratingsByItem;
    protected final int NUM_OF_RECOMMENDATIONS = 10;

    protected RecommenderSystem(Map<Integer, User> users,
                                Map<Integer, T> items,
                                List<Rating<T>> ratings) {
        this.users = users;
        this.items = items;
        this.ratings = ratings;
        this.ratingsByUser = ratings.stream().collect(groupingBy(Rating::getUserId));
        this.ratingsByItem = ratings.stream().collect(groupingBy(Rating::getItemId));
    }

    /** @return top‑10 recommended items for the given user, sorted best‑first. */
    public abstract List<T> recommendTop10(int userId);
}
