package cl.dynasty.nexusbeacon.platform.classic;
import org.bukkit.Material;
final class ClassicPaymentOption {enum Type{ITEM,EXP_LEVEL,VAULT_MONEY,NONE}final Type type;final int amount;final Material material;ClassicPaymentOption(Type type,int amount,Material material){this.type=type;this.amount=amount;this.material=material;}}
