package learners;

import Nodes.*;
import ml.ARFFParser;
import ml.Matrix;

import java.io.IOException;
import java.util.*;

public class GolfNetwork {


    public static void main(String[] args) throws IOException {
        Matrix points = ARFFParser.loadARFF(args[0]);

        NormalNode normalNode = new NormalNode();
        ConstantNode normalNodeMean = new ConstantNode(71.82);
        ConstantNode normalNodeVar = new ConstantNode(2.42);
        List<Node> normalNodeParams = new ArrayList<Node>();
        normalNodeParams.add(normalNodeMean);
        normalNodeParams.add(normalNodeVar);
        normalNode.addParameters(VariableNode.NO_PARENTS, normalNodeParams);

        InverseGammaNode ig1 = new InverseGammaNode();
        ConstantNode ig1Alpha = new ConstantNode(18);
        ConstantNode ig1Beta = new ConstantNode(66.667);
        List<Node> ig1Params = new ArrayList<Node>();
        ig1Params.add(ig1Alpha);
        ig1Params.add(ig1Beta);
        ig1.addParameters(VariableNode.NO_PARENTS, ig1Params);

        InverseGammaNode ig2 = new InverseGammaNode();
        ConstantNode ig2Alpha = new ConstantNode(18);
        ConstantNode ig2Beta = new ConstantNode(66.667);
        List<Node> ig2Params = new ArrayList<Node>();
        ig2Params.add(ig2Alpha);
        ig2Params.add(ig2Beta);
        ig2.addParameters(VariableNode.NO_PARENTS, ig2Params);

        InverseGammaNode ig3 = new InverseGammaNode();
        ConstantNode ig3Alpha = new ConstantNode(83);
        ConstantNode ig3Beta = new ConstantNode(714.29);
        List<Node> ig3Params = new ArrayList<Node>();
        ig3Params.add(ig3Alpha);
        ig3Params.add(ig3Beta);
        ig3.addParameters(VariableNode.NO_PARENTS, ig3Params);

        Network network = new Network();

        //tournament difficulty nodes
        List<NormalNode> tournamentNodes = new ArrayList<NormalNode>();
        for(int i = 0; i < 42; i++){
            NormalNode tournamentNode = new NormalNode();
            List<Node> tournamentParams = new ArrayList<Node>();
            tournamentParams.add(normalNode);
            tournamentParams.add(ig1);
            tournamentNode.addParameters(VariableNode.NO_PARENTS, tournamentParams);

            normalNode.addChild(tournamentNode);
            ig1.addChild(tournamentNode);

            tournamentNodes.add(tournamentNode);
            network.add(tournamentNode);
        }

        //Golfer error nodes
        ConstantNode zeroNode = new ConstantNode(0);
        List<Node> golferNodes = new ArrayList<Node>();
        for(int i = 0; i < 604; i++){
            NormalNode golferNode = new NormalNode();
            List<Node> golferParams = new ArrayList<Node>();
            golferParams.add(zeroNode);
            golferParams.add(ig2);
            golferNode.addParameters(VariableNode.NO_PARENTS, golferParams);

            ig2.addChild(golferNode);

            golferNodes.add(golferNode);
            network.add(golferNode);
        }

        //Swings nodes
        for(List<Double> row : points.getData()){
            int golfer = row.get(0).intValue();
            double swings = row.get(1);
            int tournament = row.get(2).intValue() - 1;


            NormalNode tournamentNode = tournamentNodes.get(tournament);
            NormalNode golferNode = (NormalNode)golferNodes.get(golfer);

            SumNode sumNode = new SumNode(tournamentNode, golferNode);

            NormalNode swingNode = new NormalNode(sumNode, ig3);
            NodeValue swingsValue = new NodeValue(swingNode, swings);
            swingNode.setNodeValue(swingsValue);
            swingNode.setObserved(true);

            ig3.addChild(swingNode);
            tournamentNode.addChild(swingNode);
            golferNode.addChild(swingNode);

            //network.add(swingNode);
        }

        network.markovChainMonteCarlo(100);

        Map<NormalNode, List<Double>> scores = new HashMap<NormalNode, List<Double>>();
        for(int i = 0; i < 100; i++){
            List<NodeValue> itScores = network.sampleNetwork(golferNodes);
            for(NodeValue score : itScores){
                if(scores.containsKey(score.getNode())){
                    scores.get(score.getNode()).add(score.getValue());
                }
                else{
                    List<Double> golferScores = new ArrayList<Double>();
                    golferScores.add(score.getValue());
                    scores.put((NormalNode)score.getNode(), golferScores);
                }
            }
        }

        TreeMap<Double, NormalNode> medianErrors = new TreeMap<Double, NormalNode>();
        for(Map.Entry<NormalNode, List<Double>> entry : scores.entrySet()){
            List<Double> sortedScores = new ArrayList<Double>(entry.getValue());
            Collections.sort(sortedScores);
            double median;
            if(sortedScores.size()%2==0){
                median = sortedScores.get(sortedScores.size()/2);
                medianErrors.put(median, entry.getKey());
            }
            else{
                median = sortedScores.get(sortedScores.size()/2);
                medianErrors.put(median, entry.getKey());
            }
        }

        Double prevMax = Double.MAX_VALUE;
        for (int i = 0; i < 10; i++){
            String golfer = points.getColumnAttributes(0).getValue((int)medianErrors.lowerEntry(prevMax).getValue().getValue());
            System.out.println("Golfer: " + golfer + "\t\tMedian: " + medianErrors.lowerKey(prevMax));
            prevMax = medianErrors.lowerKey(prevMax);
        }


    }

}