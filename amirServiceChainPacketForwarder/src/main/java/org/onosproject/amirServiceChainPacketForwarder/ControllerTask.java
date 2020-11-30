package apps.amirServiceChainPacketForwarder.src.main.java.org.onosproject.amirServiceChainPacketForwarder;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.*;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.flow.*;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;


public class ControllerTask {

    public static final String MainVirtualHostIPAddress = "192.168.1.6";
    public static final String SourceIP = "10.10.0.1";
    public static final String InitialDestinationIP = "10.10.0.2";
    public static final String SecondDestinationIP = "10.10.0.3";
    public static final String InitialDestinationName = "c916";
    public static final String SecondDestinationName = "9005";
    public static final String[] WebServersName = {InitialDestinationName, SecondDestinationName};
    public static final int UpThreshold = 20;

    class Task extends TimerTask {

        public Device getDevice() {
            return device;
        }

        public DeviceService getDeviceService() {
            return deviceService;
        }

        public long getDelay() {
            return delay;
        }

        // <Name, ContainerOBJ>
        private Map<String, Container> containerMap = new HashMap<String, Container>();

        public Map<String, Container> getContainerMap() {
            return containerMap;
        }

        public void setContainerMap(Map<String, Container> containerMap) {
            this.containerMap = containerMap;
        }


        // Previous link cost
        private long pathToInitDstPreviousBitSent = 0;

        private long pathToSecondDstPreviousBitSent = 0;


        // Previous Host and its flows
        private HostLocation previousLocation = null;


        public long[] getLinkStatusDeltaValues() {
            return linkStatusDeltaValues;
        }

        public HostLocation getPreviousLocation() {
            return previousLocation;
        }

        public void setPreviousLocation(HostLocation previousLocation) {
            this.previousLocation = previousLocation;
        }


        public class Container {
            private float cpu;
            private float memory;

            public Container(float cpu, float memory) {
                this.cpu = cpu;
                this.memory = memory;
            }

            public float getCpu() {
                return cpu;
            }

            public void setCpu(float cpu) {
                this.cpu = cpu;
            }

            public float getMemory() {
                return memory;
            }

            public void setMemory(float memory) {
                this.memory = memory;
            }
        }

        @Override
        public void run() {
            while (!isExit()) {

                // Collecting The containers' CPU/Mem data from Monitoring unit
                try {
                    for (String appName : WebServersName) {
                        float tempCpu = -1;
                        float tempMemory = -1;
                        URL URLapp = new URL("http://" + MainVirtualHostIPAddress + "/" + appName);
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(URLapp.openStream()));
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            if (inputLine.startsWith("Cpu")) {
                                tempCpu = Float.parseFloat(inputLine.split(" ")[1].replace("%", ""));
                            } else if (inputLine.startsWith("MemoryUsage")) {
                                tempMemory = Float.parseFloat(inputLine.split(" ")[1].replace("%", ""));
                            }
                        }
                        in.close();
                        if (tempCpu >= 0 && tempMemory >= 0) {
                            // Check the container does not exist
                            if (containerMap.get(appName) != null) {
                                containerMap.get(appName).setCpu(tempCpu);
                                containerMap.get(appName).setMemory(tempMemory);
                            } else {
                                containerMap.put(appName, new Container(tempCpu, tempMemory));
                            }
                        }
                    }
                } catch (IOException e) {
                    log.error(e.toString());
                }


                if (WebServersName.length == getContainerMap().size()) {
                    log.info("----------- Collecting the servers Utilization ---------------------------");
                    containerMap.forEach((key, value) -> {
                        if(key.equals("c916"))
                            log.info("Service 10.10.0.2 utilization is " + String.valueOf(value.getCpu()));
                        else
                            log.info("Service 10.10.0.3 utilization is " + String.valueOf(value.getCpu()));
                    });

                    // Both destination Host
                    Host initialDestinationHost = null;
                    Host secondDestinationHost = null;
                    boolean findInitHost = false;
                    boolean findSecondHost = false;


                    for (Host host : getHostService().getHostsByIp(IpAddress.valueOf(InitialDestinationIP))) {
                        initialDestinationHost = host;
//                        log.info("initialDestinationHost: " + initialDestinationHost.toString());
                        findInitHost = true;
                    }
                    for (Host host : getHostService().getHostsByIp(IpAddress.valueOf(SecondDestinationIP))) {
                        secondDestinationHost = host;
//                        log.info("secondDestinationHost: " + secondDestinationHost.toString());
                        findSecondHost = true;
                    }


                    // Check both destination are found
                    if (findSecondHost && findInitHost) {

//                    log.info("Both destination hosts are founded");

                        for (Host host : getHostService().getHostsByIp(IpAddress.valueOf(SourceIP))) {
//                        log.info("Source host has been founded");
                            if (getPreviousLocation() == null) {
                                setPreviousLocation(host.location());
//                            installRule(host.location(), initialDestinationHost, secondDestinationHost);
                            } else {
                                // Has vehicle's location changed?
                                if (!getPreviousLocation().deviceId().toString().equals(host.location().deviceId().toString())) {
                                    log.info("----------- Vehicle's location has changed -------------------------------");
                                    // Check the Utilization of the second host
//                                    if (containerMap.get(SecondDestinationName).getCpu() < UpThreshold) {
                                    if (true) {
                                        log.info("----------- Collecting the user data size (bit rate per second) -----------");
                                        // Calculate the ROUTING ALGORITHMS WITH DYNAMIC LINK COST(RA-DLC)
                                        // The shortest path to the service in zone 2 (newPath)
                                        Set<Path> pathsToSecondHost =
                                                getTopologyService().getPaths(getTopologyService().currentTopology(),
                                                        host.location().deviceId(),
                                                        secondDestinationHost.location().deviceId());

                                        // The shortest path to the service in zone 1 (prevPath)
                                        Set<Path> pathsToInitialHost =
                                                getTopologyService().getPaths(getTopologyService().currentTopology(),
                                                        host.location().deviceId(),
                                                        initialDestinationHost.location().deviceId());

                                        long pathToInitDstPreviousBitSentPerSecond = 0;
                                        long pathToSecondDstPreviousBitSentPerSecond = 0;

                                        for (Path pathToInitDst : pathsToInitialHost) {
                                            long cumulativeCost = 0;
                                            for (Link lk : pathToInitDst.links()) {
                                                cumulativeCost = cumulativeCost + getDeviceService().getStatisticsForPort(lk.src().deviceId(), lk.src().port()).bytesSent();
                                            }
                                            if(pathToInitDstPreviousBitSent != cumulativeCost) {
                                                pathToInitDstPreviousBitSentPerSecond = (cumulativeCost - pathToInitDstPreviousBitSent) / 3;
                                                pathToInitDstPreviousBitSent = cumulativeCost;
                                            }
                                            break;
                                        }
                                        for (Path pathToSecondDst : pathsToSecondHost) {
                                            long cumulativeCost = 0;
                                            for (Link lk : pathToSecondDst.links()) {
                                                cumulativeCost = cumulativeCost + getDeviceService().getStatisticsForPort(lk.src().deviceId(), lk.src().port()).bytesSent();
                                            }
                                            if(pathToSecondDstPreviousBitSent != cumulativeCost) {
                                                pathToSecondDstPreviousBitSentPerSecond = (cumulativeCost - pathToSecondDstPreviousBitSent) / 3;
                                                pathToSecondDstPreviousBitSent = cumulativeCost;
                                            }
                                            break;
                                        }

                                        log.info("Bit rate of the path to 10.10.0.2: " + pathToInitDstPreviousBitSentPerSecond + " bit/sec");
                                        log.info("Bit rate of the path to 10.10.0.3: " + pathToSecondDstPreviousBitSentPerSecond + " bit/sec");
                                        log.info("--------------------------------------------------------------------------");

//                                        if (pathToSecondDstPreviousBitSentPerSecond < 10 * pathToInitDstPreviousBitSentPerSecond) {
                                        if (true) {
                                            log.info("Assigning the vehicle to service in zone 2 (10.10.0.3)");
                                            // Remove rules from previous host
                                            log.info("Removing rules from previous switch");
                                            for (FlowRule flowRl : previousHostFlowRules) {
                                                flowRuleService.removeFlowRules(flowRl);
                                            }
                                            previousHostFlowRules.clear();

                                            // Set the new flow on new edge switch
                                            log.info("Adding rules to the new switch");
                                            installRule(host.location(), initialDestinationHost, secondDestinationHost);

                                            // At the end remove the previous host and add new host to it
                                            log.info("Update the vehicle location in database");
                                            setPreviousLocation(host.location());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                try {
                    Thread.sleep((getDelay() * 1000));
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }


    long[] linkStatusDeltaValues = new long[2];

    public void setLinkStatusDeltaValues(long[] linkStatusDeltaValues) {
        this.linkStatusDeltaValues = linkStatusDeltaValues;
    }

    private Set<FlowRule> previousHostFlowRules = new HashSet<>();

    void installRule(HostLocation hostLocation, Host initialHostDestination, Host secondHostDestination) {
        log.info("Installing rules");
        Set<Path> paths =
                getTopologyService().getPaths(getTopologyService().currentTopology(),
                        hostLocation.deviceId(),
                        secondHostDestination.location().deviceId());

        Path mainPath = paths.stream().findFirst().get();

        // First Rule (Transmission)
        TrafficSelector selectorSending = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchInPort(hostLocation.port())
                .build();
        TrafficTreatment treatmentSending = DefaultTrafficTreatment.builder()
                .add(Instructions.modL2Dst(secondHostDestination.mac()))
                .add(Instructions.modL3Dst(IpAddress.valueOf(SecondDestinationIP)))
                .setOutput(mainPath.src().port())
                .build();
        FlowRule flowRuleSending = DefaultFlowRule.builder()
                .forDevice(hostLocation.deviceId())
                .fromApp(getAppId())
                .withSelector(selectorSending)
                .withTreatment(treatmentSending)
                .withPriority(60010)
                .makePermanent()
                .build();
        FlowRuleOperations flowRuleOperationsSending = FlowRuleOperations.builder()
                .add(flowRuleSending)
                .build();
        flowRuleService.apply(flowRuleOperationsSending);


        // Second Rule (Receiving)
        TrafficSelector selectorReceiving = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .build();
        TrafficTreatment treatmentReceiving = DefaultTrafficTreatment.builder()
                .add(Instructions.modL2Src(initialHostDestination.mac()))
                .add(Instructions.modL3Src(IpAddress.valueOf(InitialDestinationIP)))
                .setOutput(hostLocation.port())
                .build();
        FlowRule flowRuleReceiving = DefaultFlowRule.builder()
                .forDevice(hostLocation.deviceId())
                .fromApp(getAppId())
                .withSelector(selectorReceiving)
                .withTreatment(treatmentReceiving)
                .withPriority(60009)
                .makePermanent()
                .build();
        FlowRuleOperations flowRuleOperationsReceiving = FlowRuleOperations.builder()
                .add(flowRuleReceiving)
                .build();
        flowRuleService.apply(flowRuleOperationsReceiving);


        previousHostFlowRules.add(flowRuleSending);
        previousHostFlowRules.add(flowRuleReceiving);
    }

    private long calculateBitrate(long numberOfBit) {
        if (linkStatusDeltaValues.length == 0) {

        }
        return 0L;
    }

    private FlowRuleService flowRuleService;

    public FlowRuleService getFlowRuleService() {
        return flowRuleService;
    }

    public void setFlowRuleService(FlowRuleService flowRuleService) {
        this.flowRuleService = flowRuleService;
    }

    private Iterable<FlowEntry> flowEntries;

    public Iterable<FlowEntry> getFlowEntries() {
        return flowEntries;
    }

    public void setFlowEntries(Iterable<FlowEntry> flowEntries) {
        this.flowEntries = flowEntries;
    }

    private PortNumber portNumber;

    public PortNumber getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(PortNumber portNumber) {
        this.portNumber = portNumber;
    }

    public void schedule() {
        this.getTimer().schedule(new Task(), 0, 1000);
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    private Timer timer = new Timer();

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    private Logger log;

    public boolean isExit() {
        return exit;
    }

    public void setExit(boolean exit) {
        this.exit = exit;
    }

    private boolean exit;

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    private long delay;

    public PortStatistics getPortStats() {
        return portStats;
    }

    public void setPortStats(PortStatistics portStats) {
        this.portStats = portStats;
    }

    private PortStatistics portStats;

    public Long getPort() {
        return port;
    }

    public void setPort(Long port) {
        this.port = port;
    }

    private Long port;

    public DeviceService getDeviceService() {
        return deviceService;
    }

    public void setDeviceService(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    protected DeviceService deviceService;

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }


    protected HostService hostService;

    public void setHostService(HostService hostService) {
        this.hostService = hostService;
    }

    public HostService getHostService() {
        return hostService;
    }


    protected TopologyService topologyService;

    public void setTopologyService(TopologyService topologyService) {
        this.topologyService = topologyService;
    }

    public TopologyService getTopologyService() {
        return topologyService;
    }

    private ApplicationId appId;

    public ApplicationId getAppId() {
        return appId;
    }

    public void setAppId(ApplicationId appId) {
        this.appId = appId;
    }

    private Device device;
}

