package jdoc.akka.cluster.sharding.typed;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Props;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.ClusterSharding;
import akka.cluster.sharding.typed.*;
import akka.cluster.typed.ClusterSingleton;
import akka.cluster.typed.ClusterSingletonSettings;

public class ShardingCompileOnlyTest {

  //#counter
  interface CounterCommand {}
  public static class Increment implements CounterCommand { }
  public static class GoodByeCounter implements CounterCommand { }

  public static class GetValue implements CounterCommand {
    private final ActorRef<Integer> replyTo;
    public GetValue(ActorRef<Integer> replyTo) {
      this.replyTo = replyTo;
    }
  }

  public static Behavior<CounterCommand> counter(Integer value) {
    return Behaviors.immutable(CounterCommand.class)
      .onMessage(Increment.class, (ctx, msg) -> {
        return counter(value + 1);
      })
      .onMessage(GetValue.class, (ctx, msg) -> {
        msg.replyTo.tell(value);
        return Behaviors.same();
      })
      .build();
  }
  //#counter

  public static void example() {

    ActorSystem system = ActorSystem.create(
      Behaviors.empty(), "ShardingExample"
    );

    //#sharding-extension
    ClusterSharding sharding = ClusterSharding.get(system);
    //#sharding-extension

    //#spawn
    EntityTypeKey<CounterCommand> typeKey = EntityTypeKey.create(CounterCommand.class, "Counter");
    ActorRef<ShardingEnvelope<CounterCommand>> shardRegion = sharding.spawn(
      counter(0),
      Props.empty(),
      typeKey,
      ClusterShardingSettings.create(system),
      10,
      new GoodByeCounter());
    //#spawn

    //#send
    EntityRef<CounterCommand> counterOne = sharding.entityRefFor(typeKey, "counter-`");
    counterOne.tell(new Increment());

    shardRegion.tell(new ShardingEnvelope<>("counter-1", new Increment()));
    //#send

    //#singleton
    ClusterSingleton singleton = ClusterSingleton.get(system);
    // Start if needed and provide a proxy to a named singleton
    ActorRef<CounterCommand> proxy = singleton.spawn(
      counter(0),
      "GlobalCounter",
      Props.empty(),
      ClusterSingletonSettings.create(system),
      new GoodByeCounter()
    );

    proxy.tell(new Increment());
    //#singleton
  }
}
