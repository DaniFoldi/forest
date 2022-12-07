package com.danifoldi.forest.tree.mail;

import com.danifoldi.dataverse.DataVerse;
import com.danifoldi.dataverse.data.NamespacedMultiDataVerse;
import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.collector.collector.CommandCollector;
import com.danifoldi.forest.seed.collector.collector.DependencyCollector;
import com.danifoldi.forest.seed.collector.collector.MessageCollector;
import com.danifoldi.forest.seed.collector.collector.PermissionCollector;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.forest.tree.dataverse.DataverseNamespace;
import com.danifoldi.forest.tree.paginate.PaginateTree;
import com.danifoldi.forest.tree.playersession.PlayersessionTree;
import com.danifoldi.microbase.BaseMessage;
import com.danifoldi.microbase.BasePlayer;
import com.danifoldi.microbase.BaseSender;
import com.danifoldi.microbase.Microbase;
import com.danifoldi.microbase.util.Pair;
import grapefruit.command.CommandDefinition;
import grapefruit.command.parameter.modifier.Flag;
import grapefruit.command.parameter.modifier.OptParam;
import grapefruit.command.parameter.modifier.Range;
import grapefruit.command.parameter.modifier.Source;
import grapefruit.command.parameter.modifier.string.Greedy;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@VersionCollector("1.0.0")
@DependencyCollector(tree="config", minVersion="1.0.0")
@DependencyCollector(tree="dataverse", minVersion="1.0.0")
@DependencyCollector(tree="listener", minVersion="1.0.0")
@DependencyCollector(tree="paginate", minVersion="1.0.0")
@DependencyCollector(tree="playersession", minVersion="1.0.0")
@CommandCollector("mail")
public class MailTree implements Tree {

    NamespacedMultiDataVerse<Mail> mailDataverse;

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            mailDataverse = DataVerse.getDataVerse().getNamespacedMultiDataVerse(DataverseNamespace.get(), "mail", Mail::new);
        }, Microbase.getThreadPool("mail"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> Microbase.shutdownThreadPool("mail", 1000, force));
    }

    @CommandDefinition(route="mail send", permission="forest.mail.command.mail.send", runAsync=true)
    @MessageCollector(value="forest.mail.received", replacements={"{sender}"})
    @MessageCollector(value="forest.mail.sent")
    @MessageCollector(value="forest.unknownPlayer", replacements={"{player}"})
    @PermissionCollector("forest.mail.command.mail.send")
    public void onMailSend(@Source BaseSender sender, String recipient, @Greedy String message) {
        Optional<UUID> recipientId = GrownTrees.get(PlayersessionTree.class).uuidOf(recipient).join();
        if (recipientId.isEmpty()) {
            sender.send(Microbase.baseMessage().providedText("forest.unknownPlayer").replace("{player}", recipient));
            return;
        }
        mailDataverse.add(recipientId.get(), new Mail(sender.name(), message));
        BasePlayer player = Microbase.getPlatform().getPlayer(recipientId.get());
        if (player != null) {
            player.send(Microbase.baseMessage().providedText("forest.mail.received").replace("{sender}", sender.name()));
        }
        sender.send(Microbase.baseMessage().providedText("forest.mail.sent"));
    }

    @CommandDefinition(route="mail list|read|view", permission="forest.mail.command.mail.send", runAsync=true)
    @PermissionCollector("forest.mail.command.mail.read")
    @MessageCollector(value="forest.mail.list.all.read")
    @MessageCollector(value="forest.mail.list.all.unread")
    @MessageCollector(value="forest.mail.list.all.header", replacements={"{items}", "{page}", "{pages}", "{target}"})
    @MessageCollector(value="forest.mail.list.all.entry", replacements={"{id}", "{target}", /* --- */ "{sender}", "{message}", "{status}"})
    @MessageCollector(value="forest.mail.list.all.footer", replacements={"{items}", "{page}", "{pages}", "{target}"})
    @MessageCollector(value="forest.mail.list.unread.header", replacements={"{items}", "{page}", "{pages}", "{target}"})
    @MessageCollector(value="forest.mail.list.unread.entry", replacements={"{id}", "{target}", /* --- */ "{sender}", "{message}"})
    @MessageCollector(value="forest.mail.list.unread.footer", replacements={"{items}", "{page}", "{pages}", "{target}"})
    public void onMailList(@Source BaseSender sender, @Flag(value="--all", shorthand='a') boolean all, @OptParam @Range(min=1, max=Integer.MAX_VALUE) Integer page) {
        if (page == null) {
            page = 1;
        }


        if (all) {
            List<Mail> mails = mailDataverse.get(sender.uniqueId(), page, 10).join();

            GrownTrees.get(PaginateTree.class).generatePagination(
                    // messageKeyPrefix
                    "forest.mail.list.all",
                    // page
                    page,
                    // pageSize
                    10,
                    // baseMessage, 1-based id -> entry baseMessage
                    (entry, id) -> {
                        Mail mail = mails.get(id - 1);
                        BaseMessage t = entry
                                .replace("{sender}", mail.sender)
                                .replace("{message}", mail.message);
                        if (mail.read) {
                            return t.replace("{status}", Microbase.provideMessage("forest.mail.list.all.read"));
                        } else {
                            return t.replace("{status}", Microbase.provideMessage("forest.mail.list.all.unread"));
                        }
                    },
                    // () -> itemCount
                    () -> mailDataverse.get(sender.uniqueId()).join().size(),
                    // suggestion command <page>
                    "mail list -a",
                    // {target}
                    sender.name()
            ).forEach(sender::send);
        } else {
            List<Mail> newMails = mailDataverse.filterBool(sender.uniqueId(), mailDataverse.getField("read"), false, page, 10).join();
            for (Mail newMail: newMails) {
                mailDataverse.delete(sender.uniqueId(), newMail).join();
                newMail.read = true;
                mailDataverse.add(sender.uniqueId(), newMail).join();
            }

            GrownTrees.get(PaginateTree.class).generatePagination(
                    // messageKeyPrefix
                    "forest.mail.list.unread",
                    // page
                    page,
                    // pageSize
                    10,
                    // baseMessage, 1-based id -> entry baseMessage
                    (entry, id) -> {
                        Mail mail = newMails.get(id - 1);
                        return entry
                                .replace("{sender}", mail.sender)
                                .replace("{message}", mail.message);
                    },
                    // () -> itemCount
                    () -> (int)mailDataverse.get(sender.uniqueId()).join().stream().filter(Predicate.not(Mail::isRead)).count(),
                    // suggestion command <page>
                    "mail list",
                    // {target}
                    sender.name()
            ).forEach(sender::send);
        }
    }

    @CommandDefinition(route="mail clear", permission="forest.mail.command.mail.clear", runAsync=true)
    @MessageCollector(value="forest.mail.deleted", replacements={"{count}"})
    @PermissionCollector("forest.mail.command.mail.clear")
    public void onMailDelete(@Source BaseSender sender, @Flag(value="--all", shorthand='a') boolean all) {
        List<Mail> toDelete = all ? mailDataverse.get(sender.uniqueId()).join() : mailDataverse.filterBool(sender.uniqueId(), mailDataverse.getField("read"), true).join();
        for (Mail mail: toDelete) {
            mailDataverse.delete(sender.uniqueId(), mail).join();
        }
        sender.send(Microbase.baseMessage().providedText("forest.mail.deleted").replace("{count}", String.valueOf(toDelete.size())));
    }
}
