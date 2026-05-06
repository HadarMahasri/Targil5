import java.util.*;

import static java.util.stream.Collectors.*;

/** Similarity‑based recommender with bias correction. */
class SimilarityBasedRecommender<T extends Item> extends RecommenderSystem<T> {
    private final double globalBias;
    private final Map<Integer, Double> itemBiases;
    private final Map<Integer, Double> userBiases;

    public SimilarityBasedRecommender(Map<Integer, User> users,
                                      Map<Integer, T> items,
                                      List<Rating<T>> ratings) {
        super(users, items, ratings);
        
        this.globalBias = ratings.stream()
                .mapToDouble(Rating::getRating)
                .average()
                .orElse(0.0);
        
        this.itemBiases = items.keySet().stream()
                .collect(toMap(
                        id -> id,
                        id -> ratingsByItem.getOrDefault(id, Collections.emptyList()).stream()
                                .mapToDouble(r -> r.getRating() - globalBias)
                                .average()
                                .orElse(0.0)
                ));
        
        this.userBiases = users.keySet().stream()
                .collect(toMap(
                        id -> id,
                        id -> ratingsByUser.getOrDefault(id, Collections.emptyList()).stream()
                                .mapToDouble(r -> r.getRating() - globalBias - itemBiases.get(r.getItemId()))
                                .average()
                                .orElse(0.0)
                ));
    }

    /** Dot‑product similarity; 0 if <10 shared items. */
    public double getSimilarity(int u1, int u2) {
        Map<Integer, Double> u1Ratings = ratingsByUser.getOrDefault(u1, Collections.emptyList()).stream()
                .collect(toMap(Rating::getItemId, Rating::getRating));
        Map<Integer, Double> u2Ratings = ratingsByUser.getOrDefault(u2, Collections.emptyList()).stream()
                .collect(toMap(Rating::getItemId, Rating::getRating));
                
        Set<Integer> sharedItems = u1Ratings.keySet().stream()
                .filter(u2Ratings::containsKey)
                .collect(toSet());
                
        if (sharedItems.size() < 10) return 0.0;
        
        return sharedItems.stream()
                .mapToDouble(itemId -> {
                    double r1BiasFree = u1Ratings.get(itemId) - globalBias - itemBiases.get(itemId) - userBiases.get(u1);
                    double r2BiasFree = u2Ratings.get(itemId) - globalBias - itemBiases.get(itemId) - userBiases.get(u2);
                    return r1BiasFree * r2BiasFree;
                })
                .sum();
    }

    private static class UserSim {
        final int userId;
        final double similarity;
        UserSim(int userId, double similarity) {
            this.userId = userId; this.similarity = similarity;
        }
    }
    
    private static class ItemPred {
        final int itemId;
        final double predRating;
        final int overallCount;
        ItemPred(int itemId, double predRating, int overallCount) {
            this.itemId = itemId; this.predRating = predRating; this.overallCount = overallCount;
        }
    }

    @Override 
    public List<T> recommendTop10(int userId) {
        List<UserSim> topSimilarUsers = users.keySet().stream()
                .filter(id -> id != userId)
                .map(id -> new UserSim(id, getSimilarity(userId, id)))
                .sorted(Comparator.comparingDouble((UserSim us) -> us.similarity).reversed()
                        .thenComparing(us -> us.userId))
                .limit(10)
                .collect(toList());
                
        Set<Integer> similarUserIds = topSimilarUsers.stream()
                .map(us -> us.userId)
                .collect(toSet());
                
        Map<Integer, Double> simMap = topSimilarUsers.stream()
                .collect(toMap(us -> us.userId, us -> us.similarity));
                
        Set<Integer> userRatedItems = ratingsByUser.getOrDefault(userId, Collections.emptyList()).stream()
                .map(Rating::getItemId)
                .collect(toSet());
                
        Map<Integer, List<Rating<T>>> similarUsersRatingsByItem = ratings.stream()
                .filter(r -> similarUserIds.contains(r.getUserId()))
                .collect(groupingBy(Rating::getItemId));
                
        return similarUsersRatingsByItem.entrySet().stream()
                .filter(e -> e.getValue().size() >= 5)
                .filter(e -> !userRatedItems.contains(e.getKey()))
                .map(e -> {
                    int itemId = e.getKey();
                    double weightedSum = e.getValue().stream()
                            .mapToDouble(r -> {
                                double sim = simMap.get(r.getUserId());
                                double biasFreeRating = r.getRating() - globalBias - itemBiases.get(itemId) - userBiases.get(r.getUserId());
                                return sim * biasFreeRating;
                            })
                            .sum();
                    double sumSim = e.getValue().stream()
                            .mapToDouble(r -> simMap.get(r.getUserId()))
                            .sum();
                            
                    double avgBiasFree = sumSim == 0 ? 0 : weightedSum / sumSim;
                    double predictedRating = avgBiasFree + globalBias + itemBiases.get(itemId) + userBiases.get(userId);
                    
                    int overallCount = ratingsByItem.getOrDefault(itemId, Collections.emptyList()).size();
                    return new ItemPred(itemId, predictedRating, overallCount);
                })
                .sorted(Comparator.comparingDouble((ItemPred p) -> p.predRating).reversed()
                        .thenComparing(Comparator.comparingInt((ItemPred p) -> p.overallCount).reversed())
                        .thenComparing(p -> items.get(p.itemId).getName()))
                .limit(NUM_OF_RECOMMENDATIONS)
                .map(p -> items.get(p.itemId))
                .collect(toList());
    }

    public double getGlobalBias() { return globalBias; }
    public double getItemBias(int itemId) { return itemBiases.getOrDefault(itemId, 0.0); }
    public double getUserBias(int userId) { return userBiases.getOrDefault(userId, 0.0); }
}

