package org.reveno.atp.clustering.core.components;

import org.reveno.atp.clustering.api.Address;
import org.reveno.atp.clustering.api.Cluster;
import org.reveno.atp.clustering.api.ClusterView;
import org.reveno.atp.clustering.api.message.Message;
import org.reveno.atp.clustering.core.RevenoClusterConfiguration;
import org.reveno.atp.clustering.core.api.MasterSlaveElector;
import org.reveno.atp.clustering.core.messages.VoteAck;
import org.reveno.atp.clustering.core.messages.VoteMessage;
import org.reveno.atp.clustering.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MessagingMasterSlaveElector implements MasterSlaveElector {

    public MessagingMasterSlaveElector(Cluster cluster, RevenoClusterConfiguration config) {
        this.cluster = cluster;
        this.config = config;
    }

    @Override
    public ElectionResult vote(ClusterView currentView) {
        LOG.trace("Vote [view: {}]", currentView.viewId());

        List<VoteMessage> answers = sendVoteNotifications(currentView);
        if (answers.size() == 0 || !allAcked(currentView)) {
            return revote(currentView);
        } else {
            boolean leader = answers.stream().allMatch(a -> config.priority() > a.priority);
            if (!leader && isAllSamePriority(answers)) {
                leader = answers.stream().sorted().findFirst().get().address().equals(config.currentNodeAddress());
            }
            LOG.trace("Vote finished [view: {}]", currentView.viewId());
            return new ElectionResult(leader, false);
        }
    }

    @Override
    public <T extends Message> void onMessage(T message) {
        if (message.type() == VoteMessage.TYPE) {
            votes.put(message.address(), (VoteMessage) message);
        } else if (message.type() == VoteAck.TYPE) {
            acks.put(message.address(), ((VoteAck) message).viewId);
        }
    }

    @Override
    public Set<Integer> interestedTypes() {
        return SUBSCRIPTION;
    }

    protected ElectionResult revote(ClusterView currentView) {
        LOG.trace("Revote [view: {}; nodes: {}]", currentView.viewId(), currentView.members());

        if (cluster.view().viewId() != currentView.viewId()) {
            LOG.trace("Vote aborted [view: {}]", currentView.viewId());
            return new ElectionResult(false, true);
        } else {
            return vote(currentView);
        }
    }

    protected boolean allAcked(ClusterView view) {
        cluster.gateway().send(view.members(), new VoteAck(view.viewId()), cluster.gateway().oob());
        return Utils.waitFor(() -> acks.values().containsAll(view.members()) && acks.entrySet()
                        .stream()
                        .filter(kv -> view.members().contains(kv.getKey()))
                        .map(Map.Entry::getValue)
                        .collect(Collectors.groupingBy(o -> o, Collectors.counting())).size() == 1,
                config.revenoTimeouts().ackTimeout());
    }

    protected List<VoteMessage> sendVoteNotifications(ClusterView view) {
        VoteMessage message = new VoteMessage(view.viewId(), config.priority());
        cluster.gateway().send(view.members(), message, cluster.gateway().oob());

        return waitForAnswers(view);
    }

    protected List<VoteMessage> waitForAnswers(ClusterView view) {
        Predicate<VoteMessage> f = m -> m.viewId == view.viewId() && view.members().contains(m.address());
        if (!Utils.waitFor(() ->
                votes.values().stream().filter(f).count() == view.members().size(), config.revenoTimeouts().voteTimeout())) {
            return Collections.emptyList();
        }

        return votes.values().stream().filter(f).collect(Collectors.toList());
    }

    protected boolean isAllSamePriority(List<VoteMessage> answers) {
        return answers.stream().collect(Collectors.groupingBy(o -> o.priority, Collectors.counting())).size() != answers.size();
    }

    protected Cluster cluster;
    protected RevenoClusterConfiguration config;
    protected Map<Address, VoteMessage> votes = new ConcurrentHashMap<>(1 << 6);
    protected Map<Address, Long> acks = new ConcurrentHashMap<>(1 << 6);

    protected static final Logger LOG = LoggerFactory.getLogger(MessagingMasterSlaveElector.class);
    protected static final Set<Integer> SUBSCRIPTION = new HashSet<Integer>() {{
        add(VoteAck.TYPE);
        add(VoteMessage.TYPE);
    }};
}
