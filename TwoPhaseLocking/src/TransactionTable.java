public class TransactionTable
{
	public static int TS = 0;
	public int trans_timestamp;
	public String trans_state;
	public String items_locked;
	public String waiting_trans;

	public String[] filedata = new String[100];

	//Default constructor
	public TransactionTable() {}

	public TransactionTable(String state, String items_locked)
	{
		this.trans_timestamp = ++TS;
		this.trans_state = state;
		this.items_locked = items_locked;
		this.waiting_trans = "";
	}

	public void ReadTransactions()
	{
		for (int i = 0; i < TwoPhaseLocking.data.length; i++)
		{
			filedata[i] = TwoPhaseLocking.data[i];
		}
		int i = 0;
		while (filedata[i] != null)
		{
			perform_action(filedata[i]);
			i++;
		}
	}
	
	public void perform_action(String item)
	{
		switch (item.substring(0, 1)) 
		{
		case "b": 
			TransactionTable T = new TransactionTable("Active", "none");
			int tid = Integer.parseInt(item.substring(1, item.indexOf(";")));
			TwoPhaseLocking.transMap.put(tid, T);
			System.out.println("Begin Transaction: T" + tid + '\n');			
			break;

		case "r":
			tid = Integer.parseInt(item.substring(1, item.indexOf("(")));
			LockTable L = new LockTable();
			L.Set_transid_RL(tid);
			String itemname = item.substring(item.indexOf("(") + 1, item.indexOf(")"));

			// Check if item does not exist in lock table -> insert into lock table
			if (!TwoPhaseLocking.lockMap.containsKey(itemname))
			{
				TwoPhaseLocking.lockMap.put(itemname, L);
				System.out.println("T" + tid + " has a read lock on item " + itemname + '\n');
				TwoPhaseLocking.transMap.get(tid).setItems_locked(itemname);
			}
			// Check if exists in lock table -> Check lock mode (WL/RL) item holding
			else
			{
				for (String key : TwoPhaseLocking.lockMap.keySet())
				{
					if (key.equals(itemname))
					{
						// T(j) has holding Write Lock and T(i) is requesting Read Lock on Item
						if ((TwoPhaseLocking.lockMap.get(itemname).Get_transid_WL()) != 0)
						{
							int timestamp_requesting_trans = TwoPhaseLocking.transMap.get(tid).getTrans_timestamp();
							int id_itemHolding_trans = TwoPhaseLocking.lockMap.get(itemname).Get_transid_WL();
							int timestamp_itemHolding_trans = TwoPhaseLocking.transMap.get(id_itemHolding_trans).getTrans_timestamp();
							// Check Wound-Wait if T(i) < T(j) --> Abort T(j) --> Else Wait T(i)
							// Abort Transaction T(j) --> Release All Holding Locks --> Change State to Aborted in Transaction Table --> Add entry in Lock Table							
							if (timestamp_requesting_trans < timestamp_itemHolding_trans)
							{
								System.out.println("T" + id_itemHolding_trans + " aborted as per Wound-Wait Protocol\n" );
								System.out.println("Transaction " + id_itemHolding_trans + " is Aborted" + '\n');
								release(id_itemHolding_trans);
								TwoPhaseLocking.transMap.get(id_itemHolding_trans).setTrans_state("Aborted");
								TwoPhaseLocking.lockMap.get(itemname).Set_transid_RL(tid);
								System.out.println("T" + tid + " has a read lock on item " + itemname + '\n');
								TwoPhaseLocking.transMap.get(tid).setItems_locked(itemname);
							}
							// Move Transaction T(j) to Queue --> Change State to Block in Transaction Table --> Add entry in Lock Table for Waiting Transaction
							else
							{
								System.out.println("Item " + key + " is Write locked and not available! \n");
								enqueue(tid);
								System.out.println("T" + tid + " is waiting for Read Lock on " + itemname + '\n');
								TwoPhaseLocking.transMap.get(id_itemHolding_trans).appendWaiting_trans("r"+Integer.toString(tid)+"("+itemname+")");
								TwoPhaseLocking.transMap.get(tid).setTrans_state("Blocked");
								TwoPhaseLocking.lockMap.get(itemname).Set_trans_waiting_read(tid);
//								TwoPhaseLocking.lockMap.get(itemname).Set_trans_waiting_write(tid);
							}
						}
						// T(j) has holding Read Lock and T(i) is requesting Read Lock on Item
						else if ((TwoPhaseLocking.lockMap.get(itemname).Get_transid_RL()) != 0)
						{
							TwoPhaseLocking.lockMap.get(itemname).Set_transid_RL(tid);
							System.out.println("T" + tid + " has a read lock on item " + itemname + '\n');
							TwoPhaseLocking.transMap.get(tid).setItems_locked(itemname);
						}
						// Item is in Lock Table but does not Holding Any Locks
						else
						{
							TwoPhaseLocking.lockMap.get(itemname).Set_transid_RL(tid);
							System.out.println("T" + tid + " has a read lock on item " + itemname + '\n');
							TwoPhaseLocking.transMap.get(tid).setItems_locked(itemname);
						}
					}
				}
			}
			break;

		case "w":
			String itemname1 = item.substring(item.indexOf("(") + 1, item.indexOf(")"));
			tid = Integer.parseInt(item.substring(1, item.indexOf("(")));
			LockTable L1 = new LockTable();
			L1.Set_transid_WL(tid);

			// Check if item does not exist in lock table -> insert into lock table
			if (!TwoPhaseLocking.lockMap.containsKey(itemname1))
			{
				TwoPhaseLocking.lockMap.put(itemname1, L1);
				System.out.println("T" + tid + " has a write lock on item " + itemname1 + '\n');
				TwoPhaseLocking.transMap.get(tid).setItems_locked(itemname1);
			}
			// Check if exists in lock table -> Check lock mode (WL/RL) item holding
			else
			{
				for (String key : TwoPhaseLocking.lockMap.keySet())
				{
					if (key.equals(itemname1))
					{
						// T(j) has holding Write Lock and T(i) is requesting Write Lock on Item
						if ((TwoPhaseLocking.lockMap.get(itemname1).Get_transid_WL()) != 0)
						{
							int timestamp_requesting_trans = TwoPhaseLocking.transMap.get(tid).getTrans_timestamp();
							int id_itemHolding_trans = TwoPhaseLocking.lockMap.get(itemname1).Get_transid_WL();
							int timestamp_itemHolding_trans = TwoPhaseLocking.transMap.get(id_itemHolding_trans).getTrans_timestamp();
							// Check Wound-Wait if T(i) < T(j) --> Abort T(j) --> Else Wait T(i)
							// Abort Transaction T(j) --> Release All Holding Locks --> Change State to Aborted in Transaction Table --> Add entry in Lock Table
							if (timestamp_requesting_trans < timestamp_itemHolding_trans)
							{
								System.out.println("T" + id_itemHolding_trans + " aborted as per Wound-Wait Protocol\n" );
								System.out.println("Transaction " + id_itemHolding_trans + " is Aborted" + '\n');
								release(id_itemHolding_trans);
								TwoPhaseLocking.transMap.get(id_itemHolding_trans).setTrans_state("Aborted");
								TwoPhaseLocking.lockMap.get(itemname1).Set_transid_WL(tid);
								System.out.println("T" + tid + " has a write lock on item " + itemname1 + '\n');
								TwoPhaseLocking.transMap.get(tid).setItems_locked(itemname1);
							}
							// Move Transaction T(j) to Queue --> Change State to Block in Transaction Table --> Add entry in Lock Table for Waiting Transaction
							else
							{
								enqueue(tid);
								TwoPhaseLocking.transMap.get(id_itemHolding_trans).appendWaiting_trans("w"+Integer.toString(tid)+"("+itemname1+")");
								TwoPhaseLocking.transMap.get(tid).setTrans_state("Blocked");
								TwoPhaseLocking.lockMap.get(itemname1).Set_trans_waiting_write(tid);
								System.out.println("T" + tid + " is waiting for Write lock on " + itemname1 + '\n');
							}
						}
						else
						{
							// T(j) has holding Read Lock and T(i) is requesting Write Lock on Item
							if ((TwoPhaseLocking.lockMap.get(itemname1).Get_transid_RL()) != 0)
							{
								// If T(i) holds a Read Lock on Item, --> No Other T(j) holds a Read Lock on Item --> Upgrade to Write Lock
								int len_transid_RL = (int) (Math.log10(TwoPhaseLocking.lockMap.get(itemname1).Get_transid_RL()) + 1);
								if (len_transid_RL < 2 && TwoPhaseLocking.lockMap.get(itemname1).Get_transid_RL() == tid)
								{
									System.out.println("T" + tid + " is Upgrading read lock to write lock on item " + itemname1 + '\n');
									TwoPhaseLocking.lockMap.get(itemname1).replace_transid_RL();
									TwoPhaseLocking.lockMap.get(itemname1).Set_transid_WL(tid);
								}
								// Force T(i) to Wait Until All Other Transactions T(j) that hold Read Locks on Item Release their Locks
								else
								{
									enqueue(tid);
									int holdingId = TwoPhaseLocking.lockMap.get(itemname1).Get_transid_RL();
									int temp = 0;
									for(int i=0; i<Math.log10(holdingId); i++)
									{
										temp = Integer.parseInt(Integer.toString(holdingId).substring(i, i+1));
										if (temp != tid)
											break;
									}
									TwoPhaseLocking.transMap.get(temp).appendWaiting_trans("w"+Integer.toString(tid)+"("+itemname1+")");
									TwoPhaseLocking.transMap.get(tid).setTrans_state("Blocked");
									TwoPhaseLocking.lockMap.get(itemname1).Set_trans_waiting_write(tid);
									System.out.println("T" + tid + " is waiting for write lock on " + itemname1 + '\n');
								}
							}
							// Item is in Lock Table but does not Holding Any Locks
							else
							{
								System.out.println("T" + tid + " has a write lock on item " + itemname1 + '\n');
								TwoPhaseLocking.lockMap.get(itemname1).replace_transid_RL();
								TwoPhaseLocking.lockMap.get(itemname1).Set_transid_WL(tid);
								TwoPhaseLocking.transMap.get(tid).setItems_locked(itemname1);
							}
						}
					}
				}
			}
			break;

		case "e":
			tid = Integer.parseInt(item.substring(1, 2));
			if (TwoPhaseLocking.transMap.get(tid).getTrans_state().equals("Blocked"))
			{
				System.out.println("Blocked Transaction " + tid + " is Aborted" + '\n');
				release(tid);
			}
			if (TwoPhaseLocking.transMap.get(tid).getTrans_state().equals("Aborted"))
			{
				System.out.println("Ignore! Transaction " + tid + " is already Aborted" + '\n');
			}
			if (TwoPhaseLocking.transMap.get(tid).getTrans_state().equals("Active"))
			{
				System.out.println("Active Transaction " + tid + " is Committed" + '\n');
				release(tid);
			}
			break;
		}
	}

	//........................Get Functions.............................................
	public String getItems_locked()
	{
		return this.items_locked;
	}
	public String getTrans_state()
	{
		return this.trans_state;
	}
	public int getTrans_timestamp()
	{
		return this.trans_timestamp;
	}
	public String getWaiting_trans()
	{
		return this.waiting_trans;
	}

	//........................Set Functions.............................................
	public void setItems_locked(String item)
	{
		if (this.items_locked.equals("none"))
		{
			this.items_locked = item;
		}
		else
			this.items_locked = this.items_locked + item;
	}
	public void setTrans_state(String state)
	{
		this.trans_state = state;
	}
	public void setWaiting_trans(String item)
	{
		this.waiting_trans = item;
	}
	
	//........................Append Waiting Transactions...............................
	public void appendWaiting_trans(String item)
	{
		this.waiting_trans += item;
	}
	
	//........................Queue Transactions........................................
	public void enqueue(int tid)
	{
		if (!TwoPhaseLocking.q.contains(tid))
			TwoPhaseLocking.q.add(tid);
	}

	//........................DeQueue Transactions......................................
	public void dequeue(int tid)
	{
		TwoPhaseLocking.q.remove(tid);
	}
	
	//........................Replace Lock......................................
	private void replaceItems_locked()
	{
		this.items_locked = "";
	}

	//........................Release Locks.............................................
	public void release(int tid)
	{
		String items_locked = TwoPhaseLocking.transMap.get(tid).getItems_locked();
		if (items_locked.equals("none"))
		{
			System.out.println("All items released");
		}
		else
		{
			TwoPhaseLocking.transMap.get(tid).replaceItems_locked();
			TwoPhaseLocking.transMap.get(tid).setItems_locked("none");
			TwoPhaseLocking.transMap.get(tid).setTrans_state("Committed");
			String waiting = TwoPhaseLocking.transMap.get(tid).getWaiting_trans();
			System.out.println("====== Waiting : " + waiting);
			TwoPhaseLocking.transMap.get(tid).setWaiting_trans("");

			for (String key : TwoPhaseLocking.lockMap.keySet())
			{
				TwoPhaseLocking.lockMap.get(key).release_transid_lock(tid);
			}
			dequeue(tid);
			System.out.println("All items released by the transaction T" + tid + '\n');
			
			if (waiting.length() > 0)
			{
				String strId = waiting.substring(1, 2);
				int id = Integer.parseInt(strId);
				TwoPhaseLocking.transMap.get(id).setTrans_state("Active");
				if (waiting.length() == 5)
				{
					if (TwoPhaseLocking.q.contains(id))
						this.perform_action(waiting);
				}
				else if (waiting.lastIndexOf(strId) > 5)
				{
					if (TwoPhaseLocking.q.contains(id))
					{
						this.perform_action(waiting.substring(0, 5));
						this.perform_action(waiting.substring(5, 10));
					}
					if (waiting.length() > 10)
						TwoPhaseLocking.transMap.get(id).appendWaiting_trans(waiting.substring(10));
				}
				else
				{
					if (TwoPhaseLocking.q.contains(id))
						this.perform_action(waiting.substring(0, 5));
					TwoPhaseLocking.transMap.get(id).appendWaiting_trans(waiting.substring(5));
				}
			}
		}
	}
}