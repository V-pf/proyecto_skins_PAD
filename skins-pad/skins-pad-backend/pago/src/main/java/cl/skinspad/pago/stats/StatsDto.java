package cl.skinspad.pago.stats;

import java.util.List;
import java.util.Map;

public record StatsDto(String timezone, String from, String to, String compareFrom, String compareTo,
    Summary current, Summary previous, List<Bucket> timeline, List<Group> categories,
    List<Rank> skins, List<Rank> sellers, List<Group> methods, List<Group> funnel,
    List<Group> publicationStates, List<Group> rejectionReasons, List<Group> wear, List<Group> grades,
    List<Group> stattrak, Attributes attributes, Users users, List<Alert> alerts, List<Sale> sales,
    List<Option> categoryOptions, List<String> warnings, Map<String,String> errors) {
  public record Summary(Long gross, Long net, Long commission, long operations, Long ticket,
      long units, Double conversion, Double averageCommission, long created) {}
  public record Bucket(String date, Long gross, long operations) {}
  public record Group(String key, String label, long count, Long amount) {}
  public record Rank(String key, String label, String image, long units, Long income, String state) {}
  public record Attributes(Double floatMin, Double floatMax, Double floatAverage, Long priceMin, Long priceMax, Long priceMedian) {}
  public record Users(long registered, long sellers, long buyers, double sellerPercent) {}
  public record Alert(String kind, String label, long count, String href) {}
  public record Sale(long paymentId, long productId, String date, String name, String image,
      String category, String sellerId, String method, Long net, Long gross, String state) {}
  public record Option(long id, String name) {}
}
