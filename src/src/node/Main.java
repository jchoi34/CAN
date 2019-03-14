package node;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {

	public static void main(String[] args) {
		String line = "";
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		Registry local;
		Compute node;
		String[] parameters;
		PathHistory history;
		String result;
		Iterator<String> iter;

		try {
			local = LocateRegistry.getRegistry(args[0]);
			node = (Compute) local.lookup("Node");
			System.out.println(local.toString());
		} catch (RemoteException|NotBoundException r) {
			r.printStackTrace();
			return;
		}

		while (line != null && !line.toLowerCase().equals("exit")) {
			System.out.println("Enter a command or type exit to quit. For a list of commands type help.");
			try {
				line = input.readLine();
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}
			parameters = parseInput(line);
			if (parameters != null) {
				// parameters[0] = command; parameters[1] = node; parameters[2] = keyword
				try {
					result = "";
					if (parameters[0].equals("insert")) {
						history = node.insertKeyword(parameters[2], node.hash(parameters[2]),
								new PathHistory(new HashSet<>(10), new Stack<>()));
						while (!history.path.empty()) {
							result += history.path.pop();
						}
						System.out.println(result);
					} else if (parameters[0].equals("search")) {
						history = node.searchKeyword(parameters[2], node.hash(parameters[2]),
								new PathHistory(new HashSet<>(10), new Stack<>()));
						if(history.result.equals("found")) {
							while (!history.path.empty()) {
								result += history.path.pop();
							}
						} else {
							System.out.println(parameters[2] + " not found.");
						}
						System.out.println(result);
					} else if (parameters[0].equals("view")) {
						if (!parameters[1].equals("")) {
							history = node.view(new PathHistory(), parameters[1]);
							System.out.println(history.result);
						} else {
							history = node.view(new PathHistory(new HashSet<>(10)));
							iter = history.results.iterator();
							while (iter.hasNext()) {
								System.out.println(iter.next());
							}
						}
					} else if (parameters[0].equals("join")) {
						if (!parameters[1].equals("")) {
							result = node.join(parameters[1]);
						} else {
							result = node.join();
						}
						System.out.println(result);
					} else if (parameters[0].equals("leave")) {
						System.out.println(node.leave());
					} 
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
		}
	}

	public static String[] parseInput(String command) {
		String temp = "";
		boolean search = false, insert = false;
		int len = command.length();
		if (len < 4) {
			System.out.println("Invalid command. Type \"help\" for a list of commands.");
			return null;
		} else if (len >= 6) {
			temp = command.substring(0, 6);
			if ((search = temp.equals("search")) && len < 12) {
				System.out.println(
						"search must follow the format \"search <keyword>\" where keyword is 5 charaters or greater");
				return null;
			} else if ((insert = temp.equals("insert")) && len < 12) {
				System.out.println(
						"insert must follow the format \"insert <keyword>\" where keyword is 5 charaters or greater.");
				return null;
			}
		}

		String node = "", keyword = "";
		if ((search || insert) && command.charAt(6) == ' ') {
			keyword = command.substring(7);
			command = temp;
		} else if (command.substring(0, 4).equals("view")) {
			if (len == 10 && command.charAt(4) == ' ') {
				node = command.substring(5, 10);
			} else if (len != 4) {
				System.out.println(
						"View command must be in the form \"view n####\" or \"view\" where " + "# is a digit from 0-9");
				return null;
			}
			command = "view";
		} else if (command.substring(0, 4).equals("join")) {
			if (len == 10  && command.charAt(4) == ' ') {
				node = command.substring(5, 10);

			} else if (len != 4) {
				System.out.println(
						"Join command must be in the form \"join n####\" or \"join\" where " + "# is a digit from 0-9");
				return null;
			}
			command = "join";
		} else if (len > 4 && command.substring(0, 5).equals("leave")) {
			if (len != 5) {
				System.out.println("Leave command must be in the form \"leave\".");
				return null;
			}
			command = "leave";
		} else if(command.equals("help")) {
			System.out.println("1)\n" +
					"Insert command:\n" +
					"\n" +
					"insert <keyword>\n" +
					"\n" +
					"* Keyword must be 5 letters or more\n" +
					"\n" +
					"2)\n" +
					"Search command:\n" +
					"\n" +
					"search <keyword>\n" +
					"\n" +
					"* Keyword must be 5 letters or more\n" +
					"\n" +
					"3)\n" +
					"View command:\n" +
					"\n" +
					"view n####\n" +
					"\n" +
					"Or\n" +
					"\n" +
					"view\n" +
					"\n" +
					"Where # is a digit from 0 - 9\n" +
					"\n" +
					"4)\n" +
					"Join command:\n" +
					"\n" +
					"join n####\n" +
					"\n" +
					"Or\n" +
					"\n" +
					"join\n" +
					"\n" +
					"Where # is a digit from 0 - 9\n" +
					"\n" +
					"5)\n" +
					"Leave command:\n" +
					"\n" +
					"leave\n");
		}
		else if (!command.equals("exit")) {
			System.out.println("Invalid command. Type \"help\" for a list of commands.");
			return null;
		}
		String[] parameters = { command, node, keyword };

		return parameters;
	}
}
