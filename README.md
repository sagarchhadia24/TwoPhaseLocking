# TwoPhaseLocking


Programming Language: JAVA

 TRANSACTION TABLE: The fields to be stored are,
o Transaction_ID
o Transaction_state (active, blocked/waiting, aborted/cancelled, committed)
o Transaction_timestamp
o List of items locked by the transaction
o Waiting Transaction List

 LOCK TABLE: The fields to be stored are,
o Item_name
o Lock_state(read/share locked, write/exclusive lock)
o Locking_Transaction_id


Program Pseudo Code:
While ( !EOF )
{
Read input file line by line
Switch (“Read first char of line”)
{
Case “b”:
Add new entry in transaction table (Transaction_state = Active, items_locked = none)
tid = int (substring staring from 1st char till ";")
Put tansactin id into HashMap ‐> put(tid, T);
print "Begin Transaction: tid"
break;
Case "r":
tid = int (substring staring from 2nd char till "(")
itemname = (extract itemname from line between "( )")
// Check if item does not exist in lock table ‐> insert into lock table
if (!itemname)

{
Update lock table (itemname, RL)
print "T 'tid' has a read lock on item 'itemname'"
Set item locked in transaction table
}
// Check if item exists in lock table ‐> yes:check read/write lock
else
{
Search itemname in lock table
Check lock status held by item
if (WL)
{
TS_requesting_trans = TS(tid)
TS_itemHolding_trans = TS_WL(itemname)
if (TS_requesting_trans < TS_itemHolding_trans)
{
print "T 'id_itemHolding_trans' aborted as per Wound‐Wait"
Abort T(itemname)
release all item locked by transaction;
Set Transaction_state = aborted/cancelled
Give read lock to requesting item
print "T 'id_requesting_trans' has a read lock on item"
Set item locked in transaction table
}
else
{
print "Item 'itemname' is Write locked and not available!"
enqueue(tid);
Set Transaction_state = blocked/waiting
Add into wating queue in tansaction table
Add entry in lock table item is waiting for read lock
}
}
if (RL)
{
Set RL on 'itemname' of T 'tid'
print "T 'tid' has a read lock on item 'itemname'"
Set item locked in transaction table
}
}
break;
Case "w":
tid = int (substring staring from 2nd char till "(")
itemname = (extract itemname from line between "( )")
// Check if item does not exist in lock table ‐> insert into lock table
if (!itemname)
{
Update lock table (itemname, WL)
print "T 'tid' has a write lock on item 'itemname'"
}
// Check if item exists in lock table ‐> yes:check read/write lock
else
{
Search itemname in lock table
Check lock status held by item
if (WL)
{
TS_requesting_trans = TS(tid)
TS_itemHolding_trans = TS_WL(itemname)
if (TS_requesting_trans < TS_itemHolding_trans)
{
print "T 'id_itemHolding_trans' aborted as per Wound‐Wait"
Abort T(itemname)
release all item locked by transaction;
Set Transaction_state = aborted/cancelled
Give read lock to requesting item
print "T 'id_requesting_trans' has a read lock on item"
Set item locked in transaction table
}
else
{
print "Item 'itemname' is Write locked and not available!"
enqueue(tid);
Set Transaction_state = blocked/waiting
Add into wating queue in tansaction table
Add entry in lock table item is waiting for read lock
}
}
if (RL)
{
if no other transaction holding lock on the 'itemname'
Upgrade to WL
print "T 'tid' is Upgrading read lock to write lock on item
'itemname'"
else
enqueue(tid);
Wait untill holding transaction not release lock on item
Add into wating queue in tansaction table
Set Transaction_state = blocked/waiting
print "T 'tid' is waiting for write lock on 'itemname'"
}
}
break;
Case “e”:
tid = int (substring staring from 1st char till ";")
if ( Transaction_state(tid) == "active")
print "Transaction 'tid' is Committed"
release(tid);
if ( Transaction_state(tid) == "blocked/waiting")
print "Blocked Transaction 'tid' is Aborted"
release(tid);
if ( Transaction_state(tid) == "aborted/cancelled")
print "Ignore! Transaction 'tid' is already Aborted"
release(tid);
break;
}
}
