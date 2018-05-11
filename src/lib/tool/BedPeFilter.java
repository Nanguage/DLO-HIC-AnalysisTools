package lib.tool;

import lib.unit.InterAction;

import java.util.ArrayList;

public class BedPeFilter {
    private ArrayList<InterAction> FilterList;
//    private ArrayList<InterAction> FilteredList = new ArrayList<>();

    public BedPeFilter(ArrayList<InterAction> filterlist) {
        FilterList = filterlist;
    }

    public Boolean Run(InterAction action) {
        for (int j = 0; j < FilterList.size(); j++) {
            if (action.getLeft().Chr.Name.equals(FilterList.get(j).getLeft().Chr.Name) && action.getRight().Chr.Name.equals(FilterList.get(j).getRight().Chr.Name)) {
                if (action.IsBelong(FilterList.get(j))) {
                    FilterList.get(j).Count--;
                    if (FilterList.get(j).Count == 0) {
                        FilterList.remove(j);
                    }
//                    FilteredList.add(action);
                    return true;
                }
            } else {
                return false;
            }
        }
        return false;
    }

//    public ArrayList<InterAction> getFilteredList() {
//        return FilteredList;
//    }

}
