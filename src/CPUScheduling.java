import java.io.*;
import java.util.*;
/**
 *@author Asli Yildirim 
 *@since 28.12.2022
 */
public class CPUScheduling {
    public static void main(String[] args) throws IOException {
        String baseFileName = args[0];
        ArrayList<Bursts> bursts = new ArrayList<>();
        ArrayList<String> processes = new ArrayList<>();
        extractingProcessesFromText(baseFileName, processes);
        bursts = fillingMapWithProcesses(processes,bursts);
        schedulingCPU(bursts,processes.size());

    }

    public static void schedulingCPU(ArrayList<Bursts> bursts, int processSize) {
        int currentTime = 0;
        ArrayList<Integer> returnValue = new ArrayList<>();
        int IDLECounter = 0;

        HashMap<String,Integer> CPUTimes = calculateEachProcessCPUTimes(bursts,processSize);
        HashMap<String,Integer> processesLastReturnTime = new HashMap<>();

        int counter = 0;
        int number = 0;
        int control = 0;


        ArrayList<String> IDLEBurst = new ArrayList<>();
        while (bursts.size() != 0){

            //Doing operation first n process
            if(currentTime == 0){
                //Adding first burst
                returnValue.add(0,currentTime+bursts.get(0).CPUBurst+ bursts.get(0).IOBurst );
                currentTime = currentTime + bursts.get(0).CPUBurst;
                processesLastReturnTime.put(bursts.get(0).PID, returnValue.get(0));
                bursts.remove(0);

                number++;

                for (int i = 1; i < processSize; i++) {
                    returnValue.add(i, currentTime + bursts.get(0).CPUBurst + bursts.get(0).IOBurst  );
                    processesLastReturnTime.put(bursts.get(0).PID, returnValue.get(i));
                    currentTime = currentTime + bursts.get(0).CPUBurst;
                    bursts.remove(0);
                    number++;


                }
                counter = 0;


            }


            //This part has probability to have IDLE
            if(bursts.get(counter).IOBurst != -1){
                returnValue.add(number, currentTime + bursts.get(counter).CPUBurst + bursts.get(counter).IOBurst  );
            }
            else {
                returnValue.add(number, currentTime + bursts.get(counter).CPUBurst );
            }


            //Calculating IDLE time
            if(currentTime < processesLastReturnTime.get(bursts.get(counter).PID)){
                //Calculating most close value for current time
                int min = calculatingReturnTime(returnValue,processSize,currentTime);
                currentTime = currentTime + ( min - currentTime); //returnValue.get(number-1)
                returnValue.remove(number);
                number--;

                if(bursts.size() != 1) {
                    IDLEBurst.add(control, bursts.get(counter).PID);
                    IDLECounter++;
                    counter++;
                    control++;
                }
                else {
                    IDLECounter++;

                }

            }
            else {
                //If we came else part than that means in this stage we don't have IDLE
                processesLastReturnTime.replace(bursts.get(counter).PID, returnValue.get(number));
                currentTime = currentTime + bursts.get(counter).CPUBurst;

                for (int i = 0; i < bursts.size(); i++) {
                    if (bursts.get(counter).PID.equals(bursts.get(i).PID)) {
                        bursts.remove(i);
                        break;
                    }
                }

                //The time and type of IDLE kept in this section is checked and the IDLE is deleted.
                if (IDLEBurst.size() != 0){
                    for (Map.Entry<String,Integer> entry: processesLastReturnTime.entrySet()) {
                        String key = entry.getKey();

                        if(IDLEBurst.contains(key) && currentTime > entry.getValue() && bursts.size()-1 <= IDLEBurst.size()){
                            IDLEBurst.removeAll(Collections.singleton(key));
                            counter = 0;
                            control--;
                            break;
                        }
                    }
                }
            }
            number++;
        }

        printingOutputs(IDLECounter,processesLastReturnTime,CPUTimes,processSize);
    }

    public static int calculatingReturnTime(ArrayList<Integer> returnValue, int processSize, int currentTime) {
        int min = Integer.MAX_VALUE;
        int a = returnValue.size() - processSize - 1 ;
        //Here the process end time is compared to the current time
        for (int i = a; i < returnValue.size()-1; i++) {
            if(returnValue.get(i) - currentTime < min){
                min = returnValue.get(i);
            }
        }
        //If current is same with return value "min"
        int index = 0;
        if(min == currentTime){
            for (int i = a; i < returnValue.size()-1; i++) {
                if(returnValue.get(a) == currentTime){
                    index= a;
                }
            }
            //Then i will extract that index
            for (int i = a; i < returnValue.size()-1; i++) {
                if(returnValue.get(i) - currentTime < min && a != index){
                    min = returnValue.get(i);
                }
            }
        }
        // It is the value that most close to current time
        return min;
    }

    public static void printingOutputs(int idleCounter, HashMap<String, Integer> processesLastReturnTime, HashMap<String, Integer> cpuTimes, int processSize) {

        double turnAroundTime = 0;
        double averageWaitingTime = 0;
        //In this part, average waiting time and turnaround time are calculated.
        for (String PID: processesLastReturnTime.keySet()) {
            averageWaitingTime = averageWaitingTime +(processesLastReturnTime.get(PID)- cpuTimes.get(PID) );
            turnAroundTime = turnAroundTime + processesLastReturnTime.get(PID);
        }
        averageWaitingTime =averageWaitingTime / processSize;
        turnAroundTime = turnAroundTime / processSize;


        System.out.println("Average turnaround time: " + turnAroundTime );
        System.out.println("Average waiting time: " + averageWaitingTime);
        System.out.println("The number of times that the IDLE process executed: " + idleCounter);
        System.out.println("HALT");
    }

    public static HashMap<String, Integer> calculateEachProcessCPUTimes(ArrayList<Bursts> bursts, int processSize) {
        HashMap<String,Integer> processes = new HashMap<>();
        for (int i = 0; i < processSize; i++) {
            processes.put(bursts.get(i).PID, 0);
        }

        for (int i = 0; i < bursts.size(); i++){
            processes.put(bursts.get(i).PID,processes.get(bursts.get(i).PID)  + bursts.get(i).CPUBurst);
        }
        return processes;
    }

    public static ArrayList<Bursts> fillingMapWithProcesses(ArrayList<String> processes, ArrayList<Bursts> bursts) {
        ArrayList<String> PID = new ArrayList<>();
        ArrayList<String> burst = new ArrayList<>();
        ArrayList<String> PIDSplit = new ArrayList<>();
        ArrayList<String> burstSplit = new ArrayList<>();
        TreeMap<Integer,String> orderingPID = new TreeMap<>();

        //Ordering process
        for (int i = 0; i < processes.size(); i++) {
            String[] processIDSplitting = processes.get(i).split(":");
            orderingPID.put(Integer.parseInt(processIDSplitting[0]),processIDSplitting[1]);
        }
        for (int i = 0; i < processes.size(); i++) {
            processes.remove(i);
        }
        processes.remove(0);
        for (Integer values: orderingPID.keySet()) {
            processes.add(values+ ":"+orderingPID.get(values));
        }
        //Splitting processes ID (PID)
        for (int i = 0; i < processes.size(); i++) {
            String[] processIDSplitting = processes.get(i).split(":\\(");
            PID.add(i, processIDSplitting[0]);
            burst.add(i, processIDSplitting[1]);
        }
        //Filling arrays with CPU burst, IO burst, PID in string type
        int i = 0;
        int counter = 0;
        while (true) {
            try {
                String[] processBurstSplitting = burst.get(i).split("\\);\\(");
                burstSplit.add(processBurstSplitting[counter]);
                PIDSplit.add(PID.get(i));
                i++;
                if (i == PID.size()) {
                    counter++;
                    i = 1;
                    i = i * counter;
                    i = 0;
                }
            } catch (IndexOutOfBoundsException e) {
                if (burst.size() == 1) {
                    break;
                }
                burst.remove(i);
                PID.remove(i);
            }

        }

        bursts = creatingBurstClass(burstSplit,PIDSplit,bursts);

        return bursts;
    }

    public static ArrayList<Bursts> creatingBurstClass(ArrayList<String> burstSplit, ArrayList<String> pidSplit, ArrayList<Bursts> bursts) {
        for (int i = 0; i < burstSplit.size(); i++) {
            String[] processIDSplitting = burstSplit.get(i).split(",");
            String[] processIO = processIDSplitting[1].split("\\)");
            Bursts splitIOCPU = new Bursts(pidSplit.get(i),Integer.parseInt(processIDSplitting[0]),Integer.parseInt(processIO[0]));
            bursts.add(splitIOCPU);

        }
        return bursts;
    }

    public static void extractingProcessesFromText(String fileName, ArrayList<String> processes) throws IOException {
        File file = new File(fileName);
        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNext()) {
                processes.add(sc.nextLine());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}

class Bursts{
    String PID;
    int CPUBurst;
    int IOBurst;
    public Bursts(String PID, int CPUBurst, int IOBurst) {
        this.PID = PID;
        this.CPUBurst = CPUBurst;
        this.IOBurst = IOBurst;
    }
}
