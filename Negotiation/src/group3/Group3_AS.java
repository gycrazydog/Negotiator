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
	private double lambda = .0,lU; // Acceptance treshold of agent at time t
	private double ACnextT = 0.9;
	private double ACtimeU = 0.7;
	private double ACconsta = 0.6;
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
		time = negotiationSession.getTime();
		//System.out.println("time  "+time);
		if(time<ACnextT){
			if(lastOpponentBidUtil> nextMyBidUtil){
				return Actions.Accept;
			}
			else
				return Actions.Reject;
		}
		else{
			double totalsessions = (double)negotiationSession.getOpponentBidHistory().size()+(double)negotiationSession.getOwnBidHistory().size();
			double avgtime;
			avgtime = time/totalsessions;
			if(1-time<avgtime)
			{
				if(lastOpponentBidUtil>ACconsta)
				return Actions.Accept;
				else
				return Actions.Reject;
			}
			else
			{
				setLambda(avgtime);
				lU = getlU(time);
				if(lastOpponentBidUtil>lU)
				return Actions.Accept;
				else
				return Actions.Reject;
			}
		}
	}
        
    private double getlU(double time) {
		return 2-Math.pow(Math.E, lambda*(time-ACnextT));
	}

	public void setLambda(double avgtime) {
    	lambda = Math.log(2-ACtimeU)/(1-ACnextT-avgtime);
    }
        
    
}