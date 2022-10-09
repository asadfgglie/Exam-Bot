package ckcsc.asadfgglie.main.exam;

import ckcsc.asadfgglie.main.Main;
import ckcsc.asadfgglie.main.Path;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class Exam extends ListenerAdapter {
    private final ArrayList<Pair<String, ArrayList<TestData>>> examData = new ArrayList<>();
    private final HashMap<Long, Object[]> testMessageData = new HashMap<>();
    private final Message mainMsg;

    private ThreadChannel threadChannel;

    private final String initMsg = "選擇下方數字開始考對應章節，選擇:100:來總複習";

    private final String cancelTest = "\u21A9";

    private boolean ready = false;

    public Exam() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(Exam.class.getClassLoader().getResourceAsStream("exam.csv")));

        String line;
        Pair<String, ArrayList<TestData>> pair;
        ArrayList<TestData> testData = null;

        while((line = reader.readLine()) != null){
            if(line.equals("---")){
                String className = reader.readLine();

                testData = new ArrayList<>();

                pair = Pair.of(className, testData);

                examData.add(pair);
            }
            else{
                assert testData != null;
                testData.add(new TestData(line.split(",")));
            }
        }

        mainMsg = Main.mainChannel.sendMessage(initMsg).complete();

        initMainMsg();
    }

//    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
//        if(event.getAuthor().isBot()){
//            return;
//        }
//
//        String msg = event.getMessage().getContentDisplay();
//        if(!msg.startsWith("!")){
//            return;
//        }
//        String[] command = msg.substring(1).split("\\s");
//
//        if(command[0].equals("考試")){
//            if(command.length == 2){
//                exam_shuffle(Integer.parseInt(command[1]) - 1, event.getChannel());
//            }
//            else {
//                exam_shuffle(event.getChannel());
//            }
//        }
//    }

    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        if(event.getUser().isBot()){
            return;
        }

        if(event.getMessageIdLong() == mainMsg.getIdLong()){
            if(event.getReactionEmote().getEmoji().equals(cancelTest)){
                if(ready) {
                    threadChannel.sendMessage("本次考試取消!").queue();
                    testMessageData.clear();
                    initMainMsg();
                    return;
                }
                return;
            }

            int index = -1;
            for (int j = 1; j < 9; j++){
                if(event.getReactionEmote().getEmoji().equals(j + "\u20E3")){
                    index = j - 1;
                    break;
                }
            }
            mainMsg.clearReactions().queue();
            if(index != -1) {
                mainMsg.editMessage("已經建立了章節" + (index + 1) + "的考卷，請寫完考卷再回來建立新考卷，或點擊:leftwards_arrow_with_hook:取消當前考試").queue();
            }
            else {
                mainMsg.editMessage("已經建立了總複習的考卷，請寫完考卷再回來建立新考卷，或點擊:leftwards_arrow_with_hook:取消當前考試").queue();
            }
            exam(index, event.getUser());
            return;
        }

        if(!testMessageData.containsKey(event.getMessageIdLong())){
            return;
        }

        TestData testData = (TestData) testMessageData.get(event.getMessageIdLong())[1];
        boolean isCorrect = event.getReactionEmote().getEmoji().equals(testData.ansNumber + "\u20E3");
        Message message = (Message) testMessageData.get(event.getMessageIdLong())[0];
        Counter counter = (Counter) testMessageData.get(event.getMessageIdLong())[2];
        counter.count++;

        if(isCorrect){
            message.addReaction("\u2705").queue();
            counter.correct++;
        }
        else {
            message.addReaction("\u274C").queue();
            testData.retryTime++;

        }
        for(int i = 1; i < 5; i++) {
            if(i != testData.ansNumber) {
                message.clearReactions(i + "\u20E3").queue();
            }
        }
        if(counter.count == counter.testLength){
            refreshMainMsg(event.getChannel(), counter);
        }
        testMessageData.remove(event.getMessageIdLong());
    }

    private void refreshMainMsg (MessageChannel channel, Counter counter) {
        channel.sendMessage(String.format("複習完畢\n得分: %4.2f\n正確題目比: %d/%d", counter.getAccuracy() * 100, counter.correct, counter.testLength)).queue();
        initMainMsg();
    }

    private void initMainMsg(){
        mainMsg.clearReactions().complete();
        mainMsg.editMessage(initMsg).complete();
        for (int j = 1; j < 9; j++){
            mainMsg.addReaction(j + "\u20E3").complete();
        }
        mainMsg.addReaction("U+1F4AF").complete();
        threadChannel = null;
    }

    private void exam (int index, User user) {
        ready = false;
        String title = (index == -1) ? "總複習" : "第" + (index + 1) + "章";
        threadChannel = mainMsg.getGuild().getTextChannelById(948575898737704970L).createThreadChannel(title + "考試").complete();

        threadChannel.addThreadMember(user).complete();
        threadChannel.sendMessage("**請等待考卷輸出完畢在作答!**\n約5~10分鐘左右!").complete();
        if(index == -1){
            for(Pair<String, ArrayList<TestData>> data : examData){
                threadChannel.sendMessage("章節: " + data.getLeft()).complete();
                sendQuestion(data.getRight(), threadChannel);
            }
        }
        else {
            Pair<String, ArrayList<TestData>> data = examData.get(index);
            threadChannel.sendMessage("章節: " + data.getLeft()).complete();
            sendQuestion(data.getRight(), threadChannel);
        }
        mainMsg.addReaction(cancelTest).queue();

        threadChannel.sendMessage(user.getAsMention() + "考卷準備完成!").queue();
    }

//    private void exam_shuffle (int parseInt, MessageChannel channel) {
//        Pair<String, ArrayList<TestData>> data = examData.get(parseInt);
//        if(data.getRight() == null){
//            channel.sendMessage("你已經複習完章節" + data.getLeft() + "了").queue();
//            return;
//        }
//        ArrayList<TestData> testData = data.getRight();
//        channel.sendMessage("章節: " + data.getLeft()).queue();
//        Random random = new Random();
//
//        int size = testData.size();
//
//        for(TestData tmp : testData){
//            if(tmp.isAns){
//                size--;
//            }
//        }
//
//        boolean has = false;
//        /**
//         * 每次答題數量
//         */
//        int count = 30;
//        ArrayList<TestData> tests = new ArrayList<>();
//        for(int i = 0; i < count; i++){
//            TestData test = testData.get(random.nextInt(testData.size()));
//            while(test.isAns && size != 0){
//                test = testData.get(random.nextInt(testData.size()));
//            }
//            if(test.isAns){
//                return;
//            }
//            size--;
//            if(size == 0){
//                examData.set(parseInt, Pair.of(data.getLeft(), null));
//            }
//
//            has = true;
//
//            tests.add(test);
//        }
//        sendQuestion(tests, channel);
//        if(!has){
//            channel.sendMessage("你已經複習完章節" + data.getLeft() + "了").queue();
//        }
//    }
//    private void exam_shuffle (MessageChannel channel){
//        int size = examData.size();
//        for(Pair<String, ArrayList<TestData>> tmp : examData) {
//            if(tmp.getRight() == null) {
//                size--;
//            }
//        }
//        if(size == 0) {
//            channel.sendMessage("你已經複習完全部章節了").queue();
//            return;
//        }
//
//        Random random = new Random();
//        int i = random.nextInt(examData.size());
//        while(examData.get(i).getRight() == null){
//            i = random.nextInt(examData.size());
//        }
//        exam_shuffle(i, channel);
//    }

    private void sendQuestion(ArrayList<TestData> tests, MessageChannel channel){
        Counter counter = new Counter(tests.size());
        for(TestData test : tests) {
            test.isAns = true;

            MessageAction action;
            if (test.getQuestion().contains("/img/")) {
                action = channel.sendMessage(new StringBuilder(test.getQuestion())
                            .delete(test.getQuestion().indexOf("/img/"), "/img/".length() + test.getQuestion().indexOf("/img/")))
                        .addFile(new File(Path.transferPath(Path.getPath() + "/img.jpg")));
            }
            else if (test.getQuestion().contains("/img2/")) {
                action = channel.sendMessage(new StringBuilder(test.getQuestion())
                            .delete(test.getQuestion().indexOf("/img2/"), "/img2/".length() + test.getQuestion().indexOf("/img2/")))
                        .addFile(new File(Path.transferPath(Path.getPath() + "/img2.jpg")));
            }
            else {
                action = channel.sendMessage(test.getQuestion());
            }

            Message message = action.complete();

            for (int j = 1; j < 5; j++) {
                message.addReaction(j + "\u20E3").complete();
            }
            if (test.ansNumber == null) {
                message.addReaction("\u26A0").complete();
            }
            else {
                testMessageData.put(message.getIdLong(), new Object[]{message, test, counter});
            }
        }
        ready = true;
    }
}

class Counter {
    public int correct = 0;
    public final int testLength;
    public int count = 0;

    public Counter(int testLength){
        this.testLength = testLength;
    }

    public double getAccuracy(){
        return (double) correct / testLength;
    }
}
