import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Ow	en Astrachan
 *
 * Revise
 */

public class HuffProcessor {

	private class HuffNode implements Comparable<HuffNode> {
		HuffNode left;
		HuffNode right;
		int value;
		int weight;

		public HuffNode(int val, int count) {
			value = val;
			weight = count;
		}
		public HuffNode(int val, int count, HuffNode ltree, HuffNode rtree) {
			value = val;
			weight = count;
			left = ltree;
			right = rtree;
		}

		public int compareTo(HuffNode o) {
			return weight - o.weight;
		}
	}

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private boolean myDebugging = true;
	
	public HuffProcessor() {
		this(false);
	}
	
	public HuffProcessor(boolean debug) {
		myDebugging = debug;
	}

	private int [] getCounts(BitInputStream in) {
		int [] counts = new int[ALPH_SIZE];
		while (true) {
			int val = in.readBits(BITS_PER_WORD);
			if (val ==-1) break;


			counts[val]++;

		}
		return counts;
	}
	private HuffNode makeTree(int []counts){
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		for (int i=0;i<counts.length;i++){
			if (counts[i]!=0){
				pq.add(new HuffNode(i,counts[i],null,null));
			}
		}
		pq.add(new HuffNode(PSEUDO_EOF,1,null,null));
		while (pq.size() > 1) {

			HuffNode left=pq.remove();
			HuffNode right= pq.remove();
			HuffNode t = new HuffNode(left.weight+right.weight,left.weight+right.weight,left,right);
			pq.add(t);
		}
		HuffNode root = pq.remove();
		return root;
	}
	private void makeEncodings (HuffNode h, String s, String [] temp){
		if (h.left ==null && h.right ==null) {
			System.out.println(h.value + "   " + s);
			temp[h.value] = s;
			return;
		}
		if (h.left!=null)
			makeEncodings(h.left,s+"0", temp);
		if (h.right!=null)
			makeEncodings(h.right,s+"1", temp);
	}

	private void writeTree(HuffNode h, BitOutputStream out) {
		while (true) {

			if (h.left == null && h.right == null) {

				out.writeBits(1,1);
				out.writeBits(BITS_PER_WORD+1, h.value);
				if (h.value==PSEUDO_EOF) break;
				return;

			} else {
				out.writeBits(1,0);
				if(h.left!=null) writeTree(h.left,out);
				if(h.right!=null) writeTree(h.right, out);
				return;
			}
		}

	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){

		int[] counts = getCounts(in);

		HuffNode root = makeTree(counts);
		in.reset();
		out.writeBits(BITS_PER_INT,HUFF_TREE);
		writeTree(root,out);
		String [] encodings = new String[ALPH_SIZE+1];
		makeEncodings(root,"",encodings);
		int i=0;
		//makeEncodings works, which means error has to be below this point
		while (true){

			int val = in.readBits(BITS_PER_WORD);
			if (val==-1) break;
			String code = encodings[val];
			System.out.println(code);
			out.writeBits(code.length(), Integer.parseInt(code,2));
			i++;
		}
		out.writeBits(encodings[PSEUDO_EOF].length(), Integer.parseInt(encodings[PSEUDO_EOF],2));
		out.close();
	}

	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out) {

		// remove all code when implementing decompress
		int bits = in.readBits(BITS_PER_INT);
		if (bits != HUFF_TREE) {
			throw new HuffException("invalid magic number" + bits);

		}
		HuffNode root = readTree(in);
		HuffNode current = root;
		while (true) {
			 bits = in.readBits(1);
			if (bits == -1) {
				throw new HuffException("bad input, no PSEUDO_EOF");
			}
			else {
				if (bits == 0) current = current.left;
				else current = current.right;

				if (current.left == null && current.right == null) {
					if (current.value == PSEUDO_EOF)
						break;   // out of loop
					else {
						out.writeBits(BITS_PER_WORD,current.value);
						current = root; // start back after leaf
					}
				}
			}
		}


		/*
		while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			out.writeBits(BITS_PER_WORD, val);
		}

		 */
		out.close();
	}

	private HuffNode readTree(BitInputStream in) {
		int bit = in.readBits(1);
		if (bit == -1) throw new HuffException("no new bits to read ");
		if (bit == 0) {
			HuffNode left = readTree(in);
			HuffNode right = readTree(in);
			return new HuffNode(0,0,left,right);
		}
		else {
			int value = in.readBits(BITS_PER_WORD+1);
			return new HuffNode(value,0,null,null);
		}
	}

}
