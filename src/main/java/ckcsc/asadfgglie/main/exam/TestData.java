package ckcsc.asadfgglie.main.exam;

import net.dv8tion.jda.api.entities.Message;

public class TestData {
    /**
     * 檢查本題是否回答過
     */
    public boolean isAns = false;
    public Integer ansNumber = null;
    /**
     * 當isAns = true時，紀錄答錯次數
     */
    public int retryTime = 0;

    public String question;
    public String[] chooses;

    public TestData(String[] context){
        question = context[0];
        chooses = new String[]{context[1], context[2], context[3], context[4]};

        try {
            ansNumber = Integer.parseInt(context[5]);
        }
        catch (NumberFormatException ignore){}
    }

    public String getQuestion() {
        StringBuilder str = new StringBuilder(question);

        str.append("\n");
        int i = 1;
        for(String tmp : chooses){
            str.append("(").append(i).append(") ").append(tmp).append("\n");
            i++;
        }

        return str.toString();
    }
}
