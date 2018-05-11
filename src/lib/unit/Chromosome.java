package lib.unit;


import org.jetbrains.annotations.NotNull;

public class Chromosome implements Comparable {
    public String Name;
    public int Size;

    public Chromosome(String s) {
        this(s, 0);
    }

    public Chromosome(String name, int size) {
        Name = name;
        Size = size;
    }


    @Override
    public int compareTo(@NotNull Object o) {
        Chromosome b = (Chromosome) o;
        return Name.compareTo(b.Name);
    }

    @Override
    public boolean equals(Object obj) {
        Chromosome b = (Chromosome) obj;
        return this.Name.equals(b.Name);
    }
}
