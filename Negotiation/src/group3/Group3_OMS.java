package group3;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import negotiator.bidding.BidDetails;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OMStrategy;
import negotiator.boaframework.OpponentModel;

/**
 * Modified a little bit to call getBid of our own opponent model
 * TeamWork: Canran Gou and Shijie Li
 * 
 * @author Canran Gou
 */
public class Group3_OMS extends OMStrategy {

	/**  when to stop updating the opponentmodel. Note that this value
	 * 	 is not exactly one as a match sometimes lasts slightly longer. */
	double updateThreshold = 1.1;
	
	/**
	 * Empty constructor for the BOA framework.
	 */
	public Group3_OMS() { }

	/**
	 * Normal constructor used to initialize the BestBid opponent model strategy.
	 * @param negotiationSession symbolizing the negotiation state.
	 * @param model used by the opponent model strategy.
	 */
	public Group3_OMS(NegotiationSession negotiationSession, OpponentModel model) {
		try {
			super.init(negotiationSession, model);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Initializes the opponent model strategy. If a value for the parameter t is given, then
	 * it is set to this value. Otherwise, the default value is used.
	 * 
	 * @param negotiationSession state of the negotiation.
	 * @param model opponent model used in conjunction with this opponent modeling strategy.
	 * @param parameters set of parameters for this opponent model strategy.
	 */
	public void init(NegotiationSession negotiationSession, OpponentModel model, HashMap<String, Double> parameters) throws Exception {
		super.init(negotiationSession, model);
		if (parameters.get("t") != null) {
			updateThreshold = parameters.get("t").doubleValue();
		} else {
			System.out.println("OMStrategy assumed t = 1.1");
		}
	}
	
	/**
	 * Returns the best bid for the opponent given a set of similarly
	 * preferred bids.
	 * 
	 * @param list of the bids considered for offering.
	 * @return bid to be offered to opponent.
	 */
	@Override
	public BidDetails getBid(List<BidDetails> allBids) {
		
		// 1. If there is only a single bid, return this bid
		if (allBids.size() == 1) {
			return allBids.get(0);
		}
		double bestUtil = -1;
		BidDetails bestBid = allBids.get(0);
		
		// 2. Check that not all bids are assigned at utility of 0
		// to ensure that the opponent model works. If the opponent model
		// does not work, offer a random bid.
		boolean allWereZero = true;
		// 3. Determine the best bid
		for (BidDetails bid : allBids) {
			//A little modification, adapted to our own model method
			double evaluation = model.getBidEvaluation(bid.getBid());
			if (evaluation > 0.0001) {
				allWereZero = false;
			}
			if (evaluation > bestUtil) {
				bestBid = bid;
				bestUtil = evaluation;
			}
		}
		// 4. The opponent model did not work, therefore, offer a random bid.
		if (allWereZero) {
			Random r = new Random();
			return allBids.get(r.nextInt(allBids.size()));
		}
		//System.out.println("aaaaaaa "+bestUtil);
		return bestBid;
	}

	/**
	 * The opponent model may be updated, unless the time is higher
	 * than a given constant.
	 * 
	 * @return true if model may be updated.
	 */
	@Override
	public boolean canUpdateOM() {
		return negotiationSession.getTime() < updateThreshold;
	}
}