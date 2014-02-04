package group3;

import java.util.HashMap;
import negotiator.boaframework.AcceptanceStrategy;
import negotiator.boaframework.Actions;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OfferingStrategy;

/**
 * This Acceptance Condition will accept an opponent bid if the utility is higher than the 
 * bid the agent is ready to present
 * 
 * Decoupling Negotiating Agents to Explore the Space of Negotiation Strategies
 * T. Baarslag, K. Hindriks, M. Hendrikx, A. Dirkzwager, C.M. Jonker
 * 
 * @author Alex Dirkzwager, Mark Hendrikx
 * @version 18/12/11
 */
public class Group3_AS extends AcceptanceStrategy {
	
	private double a;
	private double b;
        private double time;
	private double lambda0 = .5; // Lambda needs an initial value
	private double lambda = .0, lT = 0; // Acceptance treshold of agent at time t
	private double delta = .8, uMax = 1;
	private double epsilon = 0.01;
	private double eta = 0.9; 
	//
	/**
	 *
         * 
	 */
	public Group3_AS() { }
	
	public Group3_AS(NegotiationSession negoSession, OfferingStrategy strat, double alpha, double beta){
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;
		this.a =  alpha;
		this.b = beta;
	}

	@Override
	public void init(NegotiationSession negoSession, OfferingStrategy strat, HashMap<String, Double> parameters) throws Exception {
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;

		if (parameters.get("a") != null || parameters.get("b") != null) {
			a = parameters.get("a");
			b = parameters.get("b");
		} else {
			a = 1;
			b = 0;
		}
	}
	
	@Override
	public String printParameters() {
		String str = "[a: " + a + " b: " + b + "]";
		return str;
	}

	@Override
	public Actions determineAcceptability() {
		double nextMyBidUtil = offeringStrategy.getNextBid().getMyUndiscountedUtil();
		double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
		if(nextMyBidUtil > lT|| a *lastOpponentBidUtil  + b> nextMyBidUtil){
	//	if (a * lastOpponentBidUtil + b >= nextMyBidUtil && time) {
			return Actions.Accept;
		}
		return Actions.Reject;
	}
        
 	private final double beta = 1., gamma = 1., weight = 1.;
	private double sigma;
        
    public void setLambda(double time) {
		if (time == 0)
			lambda = lambda0 + (1 - lambda) * Math.pow(delta, beta);
		else
			lambda = lambda + weight * (1 - lambda) * Math.pow(sigma, (time * gamma));
	}
        
    public void setLT(double time) {
		double alpha = 1; // Linear, boulware or conceder
		if (time < lambda) {
			lT = uMax - (uMax - uMax * Math.pow(delta, 1 - lambda))
					* Math.pow(time / lambda, alpha);
		} else {
			lT = uMax * Math.pow(delta, 1 - time);
		}
	}
}