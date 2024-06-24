package subsidary;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class MLFQ_2PROCESSOR {
    static FileWriter fw;
    static int highQueueJobWorkDone = 0;
    static int mediumQueueJobWorkDone = 0;
    static int lowQueueJobWorkDone = 0;

    static class Processor{
        int id;
        boolean isFree;
        int totalLoad;
        Queue<Process> q1 = new LinkedList<>();
        Queue<Process> q2 = new LinkedList<>();
        Queue<Process> q3 = new LinkedList<>();
        public Processor(int id, boolean isFree, int totalLoad) {
            this.id = id;
            this.isFree = isFree;
            this.totalLoad = totalLoad;
        }
    }

    static class Process {
        int pid;
        int arrivalTime;
        int burstTime;
        int remainingBurstTime;
        int waitingTime;
        int turnaroundTime;
        int completionTime;
        int completedBy;
        int cotributedBY;
        public Process(int pid, int arrivalTime, int burstTime) {
            this.pid = pid;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.remainingBurstTime = burstTime;
        }
    }

    static HashMap<Process,Integer> map = new HashMap<>();
    
    public static void takeInput(int n, Process[] processes){
        // get arrival time and burst time of each process from the user
        Scanner sc = new Scanner(System.in);
        for (int i = 0; i < n; i++) {
            System.out.print("Enter arrival time and burst time for process " + (i + 1) + ": ");
            int arrivalTime = sc.nextInt();
            int burstTime = sc.nextInt();
            processes[i] = new Process(i + 1, arrivalTime, burstTime);
        }

        // sort the processes by their arrival time
        Arrays.sort(processes, Comparator.comparingInt(p -> p.arrivalTime));
    }

    public static int remainingTime(Processor processor){
        Queue<Process> q1 = processor.q1;
        Queue<Process> q2 = processor.q2;
        Queue<Process> q3 = processor.q3;
        int remainingTime = 0;

        for(Process p:q1){
            remainingTime += p.remainingBurstTime;
        }
        for(Process p:q2){
            remainingTime += p.remainingBurstTime;
        }
        for(Process p:q3){
            remainingTime += p.remainingBurstTime;
        }
        return remainingTime;
    }

    public static void run(Process[] processes, int n) throws IOException {
        Processor processor1 = new Processor(1,true,0);
        Processor processor2 = new Processor(1,true,0);

        int time1 = 0;
        int time2 = 0;

        if (n==0){
            System.out.println("Program doesnt run with 0 processes");
            return ;
        }

        int completed = 0;
        int time = 0;
        Queue<Process> mainQueue = new LinkedList<>();

        for (Process process : processes) {
            if (process.arrivalTime==0) {
                if (remainingTime(processor1)<=remainingTime(processor2)){
                    processor1.q1.offer(process);
                } else{
                    processor2.q1.offer(process);
                }
            } else{
                mainQueue.offer(process);
            }
        }
        if(processor1.q1.isEmpty() && processor2.q1.isEmpty()){
            assert mainQueue.peek() != null;
            if(mainQueue.peek().arrivalTime>0){
                time1 = mainQueue.peek().arrivalTime;
                time2 = time1;
                time = time1;
            }
        }

        for(;time>=0;time++){
            // step1: checking if number of process to be executed is complete
            if(completed>=n) break;
            boolean check1 = true;
            boolean check2 = true;

            // now seeing the code for process 1
            // this code won't until time == time1 because sometimes we add time1 + remaining time
            if(time==time1){
                // step2: at the same time checking if any process has arrived or was waiting in the main queue to enter into the highest priority queue
                while (!mainQueue.isEmpty() && mainQueue.peek().arrivalTime<=time1){
                    Process process = mainQueue.poll();
                    if (remainingTime(processor1)<=remainingTime(processor2)){
                        processor1.q1.offer(process);
                    } else{
                        processor2.q1.offer(process);
                    }
                }
                if(!processor1.q1.isEmpty()){
                    // highest priority queue has process to be executed but their arrival time > current time then we keep check = false
                    Process p = processor1.q1.peek();
                    if(p.arrivalTime>time1){
                        time1++;
                        check1 = false;
                    }
                    if (check1){
                        // check1 = true => arrival time >= current time; it tells weather cpu acted on any process at t = time1
                        if (p.remainingBurstTime <= 5) {
                            // process is completed its execution in this loop
                            p.completionTime = time1 + p.remainingBurstTime;
                            p.turnaroundTime = p.completionTime - p.arrivalTime;
                            p.waitingTime = p.turnaroundTime - p.burstTime;
                            // process has completed after current time + remaining Burst time
                            time1 += p.remainingBurstTime;
                            if (map.containsKey(p)){
                                p.waitingTime += map.get(p);
                            }
                            highQueueJobWorkDone += p.remainingBurstTime;
                            processor1.q1.poll();
                            completed++;
                            p.completedBy = 1;
                        } else {
                            p.remainingBurstTime -= 5;
                            highQueueJobWorkDone += 5;
                            time1 += 5;
                            processor1.q2.offer(processor1.q1.poll());
                        }
                    }
                }
                else if (!processor1.q2.isEmpty() && check1) {
                    // how do we know that by time = time1 process in processor 2 would have been executed
                    while (!mainQueue.isEmpty() && mainQueue.peek().arrivalTime<=time1){
                        if (remainingTime(processor1)<=remainingTime(processor2)){
                            processor1.q1.offer(mainQueue.poll());
                            check1 = false;
                        }else{
                            processor2.q1.offer(mainQueue.poll());
                        }
                    }
                    // If Queue1 is empty then we come to Queue2
                    // and if it again gets occupied in by previous step then we have go to high priority queue then if arrival time < = time
                    if (check1){
                        Process p = processor1.q2.peek();
                        if (p.remainingBurstTime <= 8) {
                            p.completionTime = time1 + p.remainingBurstTime;
                            p.turnaroundTime = p.completionTime - p.arrivalTime;
                            p.waitingTime = p.turnaroundTime - p.burstTime;
                            time1 += p.remainingBurstTime;
                            if (map.containsKey(p)){
                                p.waitingTime += map.get(p);
                            }
                            mediumQueueJobWorkDone += p.remainingBurstTime;
                            processor1.q2.poll();
                            completed++;
                            p.completedBy = 1;

                        } else {
                            p.remainingBurstTime -= 8;
                            time1 += 8;
                            mediumQueueJobWorkDone += 8;
                            processor1.q3.offer(processor1.q2.poll());
                        }
                    }
                }
                else if (!processor1.q3.isEmpty() && check1){
                    // if queue2 is empty we come to queue3
                    Process p = processor1.q3.peek();
                    while (p.remainingBurstTime!=0){
                        check1 = false;
                        while (!mainQueue.isEmpty() && mainQueue.peek().arrivalTime<=time1){
                            if (remainingTime(processor1)<=remainingTime(processor2)){
                                processor1.q2.offer(processor1.q3.poll());
                                Process process = mainQueue.poll();
                                processor1.q1.offer(process);
                                break;
                            }else{
                                processor2.q1.offer(mainQueue.poll());
                            }
                        }
                        if(time<time1) break;
                        p.completionTime = 1 + time1;
                        lowQueueJobWorkDone++;
                        p.remainingBurstTime--;
                        time1++;

                    }
                    if (check1){
                        p.turnaroundTime = p.completionTime - p.arrivalTime;
                        p.waitingTime = p.turnaroundTime - p.burstTime;
                        processor1.q3.poll();
                        if (map.containsKey(p)){
                            p.waitingTime += map.get(p);
                        }
                        completed++;
                        p.completedBy = 1;
                    }
                }
                else{
                    time1++;
                    Processor contribute = processor2;
                    contribute(contribute,1);
                }
            }

            if(time==time2){
                // Checking Again for Processor 2
                while (!mainQueue.isEmpty() && mainQueue.peek().arrivalTime<=time2){
                    if (remainingTime(processor2)<=remainingTime(processor1)){
                        Process process = mainQueue.poll();
                        processor2.q1.offer(process);
                    }else{
                        Process process = mainQueue.poll();
                        processor1.q1.offer(process);
                    }
                }
                // Processor 2
                // process the first queue
                if (!processor2.q1.isEmpty()) {
                    Process p = processor2.q1.peek();
                    if(p.arrivalTime>time2){
                        time2++;
                        check2 = false;
                    }
                    if (check2){
                        if (p.remainingBurstTime <= 5) {
                            p.completionTime = time2 + p.remainingBurstTime;
                            p.turnaroundTime = p.completionTime - p.arrivalTime;
                            p.waitingTime = p.turnaroundTime - p.burstTime;
                            time2 += p.remainingBurstTime;
                            highQueueJobWorkDone += p.remainingBurstTime;
                            processor2.q1.poll();
                            if (map.containsKey(p)){
                                p.waitingTime += map.get(p);
                            }
                            completed++;
                            p.completedBy = 2;
                        } else {
                            p.remainingBurstTime -= 5;
                            time2 += 5;
                            processor2.q2.offer(processor2.q1.poll());
                            highQueueJobWorkDone += 5;
                        }
                    }
                }
                else if (!processor2.q2.isEmpty() && check2) {
                    while (!mainQueue.isEmpty() && mainQueue.peek().arrivalTime<=time2){
                        if (remainingTime(processor2)<=remainingTime(processor1)){
                            Process process = mainQueue.poll();
                            processor2.q1.offer(process);
                            check2 = false;
                        } else{
                            Process process = mainQueue.poll();
                            processor1.q1.offer(process);
                        }
                    }
                    // If Queue1 is empty then we come to Queue2
                    if (check2){
                        Process p = processor2.q2.peek();
                        if (p.remainingBurstTime <= 8) {
                            p.completionTime = time2 + p.remainingBurstTime;
                            p.turnaroundTime = p.completionTime - p.arrivalTime;
                            p.waitingTime = p.turnaroundTime - p.burstTime;
                            time2 += p.remainingBurstTime;
                            mediumQueueJobWorkDone += p.remainingBurstTime;
                            processor2.q2.poll();
                            if (map.containsKey(p)){
                                p.waitingTime += map.get(p);
                            }
                            completed++;
                            p.completedBy = 2;
                        } else {
                            p.remainingBurstTime -= 8;
                            time2 += 8;
                            processor2.q3.offer(processor2.q2.poll());
                            mediumQueueJobWorkDone += 8;
                        }
                    }
                }
                else if (!processor2.q3.isEmpty() && check2){
                    // if queue2 is empty we come to queue3
                    Process p = processor2.q3.peek();
                    while (p.remainingBurstTime!=0){
                        check2 = false;
                        while (!mainQueue.isEmpty() && mainQueue.peek().arrivalTime<=time2){
                            if (remainingTime(processor2)<=remainingTime(processor1)){
                                processor2.q2.offer(processor2.q3.poll());
                                Process process = mainQueue.poll();
                                processor2.q1.offer(process);
                                break;
                            } else{
                                Process process = mainQueue.poll();
                                processor1.q1.offer(process);
                            }
                        }
                        if(time<time2) break;
                        p.completionTime = 1 + time2;
                        lowQueueJobWorkDone++;
                        p.remainingBurstTime--;
                        time2++;

                    }
                    if (check2){
                        p.turnaroundTime = p.completionTime - p.arrivalTime;
                        p.waitingTime = p.turnaroundTime - p.burstTime;
                        processor2.q3.poll();
                        if (map.containsKey(p)){
                            p.waitingTime += map.get(p);
                        }
                        completed++;
                        p.completedBy = 2;
                    }
                }
                else{
                    time2++;
                    Processor contribute = processor1;
                    contribute(contribute, 2);
                }
            }

        }
    }

    // Method helpes another processor if it is free and doesnt have any process running on it
    public static void contribute(Processor check,int id){
        if (!check.q1.isEmpty()){
            Process p = check.q1.peek();
            if (p.remainingBurstTime>1){
                p.remainingBurstTime--;
                highQueueJobWorkDone++;
                map.put(p,map.getOrDefault(p,0)+1);
                p.cotributedBY = id;
            }
        }else  if (!check.q2.isEmpty()) {
            Process p = check.q2.peek();
            if (p.remainingBurstTime > 1) {
                p.remainingBurstTime--;
                mediumQueueJobWorkDone++;
                map.put(p,map.getOrDefault(p,0)+1);
                p.cotributedBY = id;
            }
        }else  if (!check.q3.isEmpty()) {
            Process p = check.q3.peek();
            if (p.remainingBurstTime > 1) {
                lowQueueJobWorkDone++;
                p.remainingBurstTime--;
                map.put(p,map.getOrDefault(p,0)+1);
                p.cotributedBY = id;
            }

        }
    }

    public static void printFinalStates(Process[] processes) throws IOException {
        // print the waiting time, turnaround time, and completion time for each process
        fw.write("PID\t\tAT\t\tBT\t\tWT\t\tTurnT\t\tCompT\t\tDone By\t\tHelped By\n");
        for (Process process : processes) {
            String s = "NO ONE";
            if (process.cotributedBY!=0){
                s = "Processor"+ process.cotributedBY+"";
            }
            fw.write(process.pid + "\t\t" + process.arrivalTime + "\t\t" + process.burstTime +
                    "\t\t" + process.waitingTime + "\t\t" + process.turnaroundTime + "\t\t\t" + process.completionTime+"\t\t\tProcessor "+process.completedBy+"\t\t"+s+"\n");
        }
    }

    // Method to calculate Avg waiting time , Avg TurnAround Time and other attributes
    public static int calculateTotalCompletionTime(Process[] processes) throws IOException {
        int max = 0;
        double averageWaitingTime = 0;
        double turnAroundTime = 0;
        for(Process process : processes){
            if (process.completionTime>max){
                max = process.completionTime;
                averageWaitingTime += process.waitingTime;
                turnAroundTime += process.turnaroundTime;
            }
        }
       fw.write("Average Waiting Time : "+averageWaitingTime/processes.length+"\n");
       fw.write("Average TurnAround Time : "+turnAroundTime/processes.length+"\n");
       fw.write("Job Done by HighLevel Queue: "+highQueueJobWorkDone+"\n");
       fw.write("Job Done by MidLevel Queue: "+ mediumQueueJobWorkDone+"\n");
       fw.write("Job Done by Lowlevel Queue: "+lowQueueJobWorkDone+"\n");
        return max;
    }

    public static MLFQ_2PROCESSOR.Process[] takeTerminalInput(){
        System.out.print("Enter the number of processes: ");
        Scanner sc =new Scanner(System.in);
        int n = sc.nextInt();
        MLFQ_2PROCESSOR.Process[] processes = new MLFQ_2PROCESSOR.Process[n];
        // get arrival time and burst time of each process from the user
        for (int i = 0; i < n; i++) {
            System.out.print("Enter arrival time and burst time for process " + (i + 1) + ": ");
            int arrivalTime = sc.nextInt();
            int burstTime = sc.nextInt();
            processes[i] = new MLFQ_2PROCESSOR.Process(i + 1, arrivalTime, burstTime);
        }
        // sort the processes by their arrival time
        Arrays.sort(processes, Comparator.comparingInt(p -> p.arrivalTime));
        return processes;
    }

    public static MLFQ_2PROCESSOR.Process[] takeFileInput(){
        try {
            File Obj = new File("C:\\Users\\ekank\\Desktop\\subsidary\\input.txt");
            Scanner reader = new Scanner(Obj);
            int n = reader.nextInt();
            MLFQ_2PROCESSOR.Process[] processes = new MLFQ_2PROCESSOR.Process[n];
            for (int i = 0; i < n; i++) {
                int arrivalTime = reader.nextInt();
                int burstTime = reader.nextInt();
                processes[i] = new MLFQ_2PROCESSOR.Process(i + 1, arrivalTime, burstTime);
            }
            reader.close();
            // sort the processes by their arrival time
            Arrays.sort(processes, Comparator.comparingInt(p -> p.arrivalTime));
            return processes;
        } catch (Exception ignored){
            System.out.println("File not Found");
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);
        System.out.println("Choose the way you want to give input: \n1) terminal \n2) file ");
        int t = sc.nextInt();
        MLFQ_2PROCESSOR.Process[] processes;
        if(t==2){
            processes = takeFileInput();
        }
        else {
            processes = takeTerminalInput();
            System.out.println("check the output file for output...");
        }
        // Complete the processes running task
        run(processes, processes.length);

        //Method to print the final stats of each process
        fw = new FileWriter("C:\\Users\\ekank\\Desktop\\subsidary\\output.txt");
        printFinalStates(processes);

        // Get total Completion Time
        int max = calculateTotalCompletionTime(processes);
        fw.write("\n Total Time to Complete all process is : "+max+"\n");
        fw.close();
    }

}
