/*
COMP9417 Machine Learning
Major Project - Traffic Lights Reinforcement Learning
Beth Crane
Gill Morris
Nathan Wilson
 */

import interfaces.Car;
import interfaces.LearningModule;
import interfaces.RoadMap;
import interfaces.TrafficLight;
import utils.Coords;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final int dim = 60;
    private static final int mapWidth = dim, mapHeight = dim;
    public static void main (String[] args) {
        //(do we need to take in any arguments? my thought is perhaps we
        //    should save the learned values to a file and pass that 
        //    file as an argument if we wish to resume from a previous 
        //    trial)
        //
        //         - probably. though I'd rather see how inefficient it
        //           is to make the poor thing learn everything again
        //           each time it runs before worrying about it. -- Gill

    	int runTime = 100200;
        int quietTime = 100000;
        boolean graphicalOutput = true;
        boolean consoleOutput = false;
        boolean output = graphicalOutput || consoleOutput;
        int score = 0;

        //Initialise map, list of cars currently on map, and list of 
        //trafficlights
        RoadMap map = new RoadMapImpl();
        List<Car> cars = new ArrayList<Car>();
        List<TrafficLight> trafficLights = 
                new ArrayList<TrafficLight>();
        trafficLights.add(new TrafficLightImpl(new Coords(20, 20),false));
        trafficLights.add(new TrafficLightImpl(new Coords(20, 40),true));
        trafficLights.add(new TrafficLightImpl(new Coords(40, 20),true));
        trafficLights.add(new TrafficLightImpl(new Coords(40, 40),false));
        double trafficDensityThreshold = 0.25;
        LearningModule learningModule = new LearningModuleImpl();
        Viewer v = graphicalOutput ? new Viewer() : null;

        //Basic logic for each time step
        // - change traffic lights if required - call a function from 
        //   'learning' class to do this
        // - move cars in their current direction by velocity (modify 
        //   velocity if necessary - using CarAI)
        // - spawn cars at extremities
        // - Now that we have the new state, update the qvalue for the p
        //  previous s,a pair
        
        // TODO: no longer matters how small i make my font,
        // this loop ain't gonna fit on my screen
        // or even robert's
        for (int timeToRun = 0; timeToRun < runTime; timeToRun++) {
            RoadMap currentState = map.copyMap();
            currentState.addCars(cars);
            List<Boolean> switchedLights;
            List<Integer> states = new ArrayList<Integer>();
            List<Integer> nextStates = new ArrayList<Integer>();
            List<Integer> rewards = new ArrayList<Integer>();

            //Update the traffic lights - switch or stay
            //Get integer representing state BEFORE cars are moved
            //and lights are switched
            for (TrafficLight light: trafficLights) {
                states.add(currentState.stateCode(light));
            }
            //returns a list of true/false that the lights were 
            //switched for learning purposes
            switchedLights = learningModule.updateTrafficLights(
                    currentState, trafficLights, timeToRun
            );
            RoadMap nextState = currentState.copyMap();

            //Move cars currently on map
            List<Car> carsToRemove = new ArrayList<Car>();
            for (Car car : cars) {
                car.updateVelocity(
                    currentState.getClosestTrafficLight(
                        car, trafficLights
                    ), 
                    currentState
                );
                car.updatePosition();
                if (car.hasLeftMap(map)) {
                     carsToRemove.add(car);
                }
            }
            cars.removeAll(carsToRemove);

            //Spawn cars onto map extremities
            for (Coords roadEntrance : map.getRoadEntrances()) {
                if (
                    Math.random() <= trafficDensityThreshold &&
                    !currentState.carAt(roadEntrance)
                ) {
                    // TODO: if currentState.carAt(roadEntrance) we
                    // should probably model that there's a queue
                    // outside the map and/or fail our traffic light
                    // learner
                    Car c = new CarImpl(
                            new Coords(roadEntrance),
                            map.getStartingVelocity(roadEntrance)
                    );
                    cars.add(c);
                }
            }
            nextState.addCars(cars);

            // Updates q-values
            //calculate reward and state code for each traffic light
            for (TrafficLight light : trafficLights) {
                rewards.add(
                    learningModule.reward3(nextState.stateCode3(light, cars))
                );
                nextStates.add(nextState.stateCode3(light, cars));
            }
            //To learn we need to pass through - previous states, 
            //actions taken, rewards
            learningModule.learn(
                states, switchedLights, rewards, nextStates, 
                trafficLights
            );

            if (timeToRun >= quietTime) {
                if (graphicalOutput) {
                    v.view(map, cars, trafficLights);
                }
                if (consoleOutput) {
                    map.print(cars, trafficLights);
                }
                if (output) {
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {}
                }
                for (Car c : cars) {
                    score += c.stopped() ? -1 : 0;
                }
            }
        }
        System.out.println("Finished with an overall score of " +(float)
            score/(runTime-quietTime) + " (higher is better, 0 best)");
    }
}
