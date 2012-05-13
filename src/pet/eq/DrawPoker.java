package pet.eq;

import java.util.Arrays;

/**
 * Draw poker equity methods
 */
public class DrawPoker extends Poker {
	
	/**
	 * Calculate draw equity using random remaining cards.
	 * (Exact equity using combinatorials is too hard with more than 2 blank cards).
	 */
	public static MEquity[] equityImpl(String[][] hands, String[] blockers) {
		System.out.println("draw sample equity: " + Arrays.deepToString(hands));

		// remaining cards in deck
		final String[] deck = ArrayUtil.remove(Poker.FULL_DECK, null, hands, blockers);

		// return value
		final MEquity[] meqs = MEquityUtil.makeMEquity(hands.length, false, deck.length, false);

		// get current hand values (not equity)
		final int[] vals = new int[hands.length];
		for (int n = 0; n < hands.length; n++) {
			if (hands[n].length == 5) {
				vals[n] = value(hands[n]);
			}
		}
		MEquityUtil.updateCurrent(meqs, MEquity.HIONLY, vals);

		final String[] h = new String[5];

		// get hand values for picks
		final int c = 100000;
		final long[] pick = new long[1];
		for (int p = 0; p < c; p++) {
			pick[0] = 0;
			for (int hn = 0; hn < hands.length; hn++) {
				// could be any length
				String[] hand = hands[hn];
				for (int n = 0; n < 5; n++) {
					if (hand.length > n) {
						h[n] = hand[n];
					} else {
						h[n] = RandomUtil.pick(deck, pick);
					}
				}
				int v = value(h);
				vals[hn] = v;
			}
			MEquityUtil.updateEquity(meqs, MEquity.HIONLY, vals, null, 0);
		}

		MEquityUtil.summariseEquity(meqs, c, 0);
		return meqs;
	}

	public static void main(String[] args) {
		String[] x = new String[] { "Ah", "Ad", "2c", "3d", "4c" };
		//String[] x = new String[] { "7c", "7s", "Qs", "7d", "Jd" };
		//String[] x = new String[] { "Ah", "Kh", "Kc", "2h", "3h" };
		//String[] x = new String[] { "4c", "5h", "6d", "7s", "9h" };
		for (int n = 0; n <= 5; n++) {
			String[] y = getHand(x, n);
			System.out.println("draw " + n + " => " + Arrays.toString(y));
		}
	}

	/**
	 * Get the players drawing hand.
	 * Always returns new array.
	 */
	public static String[] getHand(String[] hand, int drawn) {
		switch (drawn) {
			case 0:
				// stand pat
				return hand.clone();
			case 1:
			case 2:
				// drawing at something
				return getDraw(hand, drawn);
			case 3:
				// if pair, return pair, otherwise high cards
				return getPair(hand);
			case 4: {
				// keep high card
				return getHigh(hand);
			}
			case 5:
				// discard all
				return new String[0];
			default:
				throw new RuntimeException("invalid drawn " + drawn);
		}
	}

	/**
	 * get the high card in the hand.
	 * always returns new array
	 */
	private static String[] getHigh(String[] hand) {
		String[] a = hand.clone();
		Arrays.sort(a, Cmp.revCardCmp);
		return new String[] { a[0] };
	}
	
	/**
	 * get best two cards in hand.
	 * always returns new array
	 */
	private static String[] getPair(final String[] hand) {
		String[] h = hand.clone();
		Arrays.sort(h, Cmp.revCardCmp);
		for (int n = 1; n < h.length; n++) {
			if (faceValue(h[n-1], true) == faceValue(h[n], true)) {
				return new String[] { h[n-1], h[n] };
			}
		}
		// return high cards
		return new String[] { h[0], h[1] };
	}
	
	/** get the flush draw, if any */
	private String[] fd(String[] hand) {
		int[] suit = new int[4];
		String[][] draw = new String[4][4];
		for (String c : hand) {
			int s = "cdhs".indexOf(suit(c));
			draw[s][suit[s]] = c;
			if (suit[s] == 4) {
				return draw[s];
			} else {
				suit[s]++;
			}
		}
		return null;
	}
	
	private String[] sd(String[] hand) {
		// get oesd, then gs
		// a23456789tjqka
		// 01111000100000
		// 01111100000000
		// 11111000000001
		// 11101000000001
		// 10000000011111
		// count, 4/5 sd, 5/5 s - avoid ace
		return null;
	}
	
	private static String[] getDraw1(final String[] hand) {
		int v = value(hand);
		int r = rank(v);
		switch (r) {
			case 0:
			case 1:
				// fd, oesd, gs, hc
			case 2:
				// 2p
			case 3:
				// tk+k
			case 4:
				// oesd, gs
			case 5:
				// hc
			case 6:
				// tk+k (or 2p, depending on aggr/num opp)
			case 7:
				// tk+k
			case 8:
				// oesd, gs
		}
		return null;
	}
	
	private static String[] getDraw2(final String[] hand) {
		int v = value(hand);
		int r = rank(v);
		switch (r) {
			case 0:
				// hc (boesd) (bfd)
			case 1:
				// p+k
			case 2:
				// p+k
			case 3:
				// tk
			case 4:
				// (boesd), hc
			case 5:
				// hc
			case 6:
				// tk
			case 7:
				// tk
			case 8:
				// (boesd), hc
		}
		return null;
	}

	/**
	 * Get the trips/st/fl draw by brute force
	 * FIXME ignores oesd to draw at higher gutty
	 */
	private static String[] getDraw(final String[] hand, final int drawn) {
		// from players pov, all other cards are possible
		final String[] deck = ArrayUtil.remove(Poker.FULL_DECK, null, null, hand);
		final String[] h = new String[5];
		final String[] maxh = new String[5 - drawn];
		final int pmax = MathsUtil.bincoff(5, 5 - drawn);
		final int qmax = MathsUtil.bincoff(deck.length, drawn);
		int maxv = 0;

		for (int p = 0; p < pmax; p++) {
			// pick kept from hand
			MathsUtil.kcomb(5 - drawn, p, hand, h, 0);
			for (int q = 0; q < qmax; q++) {
				// pick drawn from deck
				MathsUtil.kcomb(drawn, q, deck, h, 5 - drawn);
				int v = value(h);
				// ignore draws to straight flush...
				if (v > maxv && v < SF_MASK) {
					// copy new max hand
					for (int n = 0; n < 5 - drawn; n++) {
						maxh[n] = h[n];
					}
					maxv = v;
				}
			}
		}
		
		//System.out.println("hand " + Arrays.toString(maxh) + " => " + valueString(maxv));
		return maxh;
	}
	
	//
	// instance methods
	//

	@Override
	public MEquity[] equity(String[] board, String[][] hands, String[] blockers) {
		return equityImpl(hands, blockers);
	}
	
	@Override
	public int value(String[] board, String[] hole) {
		if (board != null || hole.length != 5) {
			throw new RuntimeException("invalid draw hand " + Arrays.toString(hole));
		}
		return value(hole);
	}

}